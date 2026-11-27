package com.proyecto.spring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CoworkingApiTests {
    @Autowired private MockMvc mvc;
    @Autowired private EntityManager entityManager;

    @Test
    void apiRoutesAreAccessibleWithoutLogin() throws Exception {
        mvc.perform(get("/api/miembros/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        mvc.perform(get("/api/planes"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()", is(3)));
        mvc.perform(get("/auth/perfil"))
                .andExpect(status().isNotFound());
    }

    @Test
    void servesFrontendWithoutAuthentication() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("CoworkHub")));
        mvc.perform(get("/js/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/javascript"));
        mvc.perform(get("/api/planes"))
                .andExpect(status().isOk());
    }

    @Test
    void roomBookingRejectsOverlapAndAcceptsAdjacentTime() throws Exception {
        LocalDate day = LocalDate.now().plusDays(3);
        LocalDateTime start = day.atTime(17, 0);
        postBooking(start, start.plusHours(1), 201);
        postBooking(start.plusMinutes(30), start.plusHours(2), 409)
                .andExpect(jsonPath("$.codigo", is("RN-01")));
        postBooking(start.plusHours(1), start.plusHours(2), 201);
    }

    @Test
    void validatesBookingDurationAsBusinessConflict() throws Exception {
        LocalDateTime start = LocalDate.now().plusDays(4).atTime(17, 0);
        postBooking(start, start.plusMinutes(45), 409).andExpect(jsonPath("$.codigo", is("RN-02")));
    }

    @Test
    void listsImportedCatalogAndReservations() throws Exception {
        mvc.perform(get("/api/salas"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()", is(6)));
        mvc.perform(get("/api/reservas").param("miembroId", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()", is(4)));
    }

    @Test
    void removesAuthenticationEndpointsAndPublishesOpenApi() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths['/auth/login']").doesNotExist())
                .andExpect(jsonPath("$.paths['/auth/perfil']").doesNotExist())
                .andExpect(jsonPath("$.components.securitySchemes").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.LoginRequest").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.ProfileResponse").doesNotExist());
        mvc.perform(get("/doc/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/auth/login"))
                .andExpect(status().isNotFound());
    }

    @Test
    void mapsMissingResourcesAndInvalidParametersToErrorEnvelope() throws Exception {
        mvc.perform(get("/api/no-such-resource"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("NOT_FOUND"));
        mvc.perform(get("/api/disponibilidad"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("VALIDATION_ERROR"));
    }

    @Test
    void createsMemberWithoutAccountFields() throws Exception {
        mvc.perform(post("/api/miembros").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentNumber\":\"DOC-NEW-1\",\"fullName\":\"New Member\",\"email\":\"new@example.com\",\"planId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("New Member"))
                .andExpect(jsonPath("$.username").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void calculatesFrozenServiceCostsAndFiltersAvailability() throws Exception {
        LocalDate day = LocalDate.now().plusDays(5);
        LocalDateTime start = day.atTime(12, 0);
        mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"salaId\":1,\"miembroId\":1,\"startTime\":\"" + start + "\",\"endTime\":\"" + start.plusHours(1)
                                + "\",\"attendees\":2,\"servicios\":[{\"servicioId\":1,\"quantity\":2},{\"servicioId\":2,\"quantity\":1}]}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.servicesCost").value(41.0))
                .andExpect(jsonPath("$.totalCost").value(41.0))
                .andExpect(jsonPath("$.servicios[0].appliedUnitPrice").value(8.0));

        mvc.perform(get("/api/disponibilidad")
                        .param("sedeId", "1").param("fecha", day.toString()).param("horaInicio", "10:00")
                        .param("horaFin", "11:00").param("capacidadMinima", "8").param("equipamientoIds", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()", is(2)));
    }

    @Test
    void reportsMonthlyMemberConsumption() throws Exception {
        mvc.perform(get("/api/miembros/1/consumo").param("mes", YearMonth.now().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.horasIncluidas").value(10.0))
                .andExpect(jsonPath("$.horasUsadas").exists()).andExpect(jsonPath("$.horasRestantes").exists())
                .andExpect(jsonPath("$.horasExcedentes").exists());
    }

    @Test
    void importsRequiredSeedEntityCountsAndRelations() {
        assertCount("locations", 2);
        assertCount("rooms", 6);
        assertCount("equipment", 4);
        assertCount("membership_plans", 3);
        assertCount("additional_services", 3);
        assertCount("members", 5);
        assertCount("bookings", 15);
        assertCount("booking_items", 4);

        Number suspended = (Number) entityManager.createNativeQuery(
                        "select count(*) from members where status = 'SUSPENDED'")
                .getSingleResult();
        org.junit.jupiter.api.Assertions.assertEquals(1, suspended.intValue());

        Number inactiveRooms = (Number) entityManager.createNativeQuery(
                        "select count(*) from rooms where active = false")
                .getSingleResult();
        org.junit.jupiter.api.Assertions.assertEquals(1, inactiveRooms.intValue());
    }

    @Test
    void filtersSuspendedMembersAndRejectsCatalogDuplicatesAndInUseDeletes() throws Exception {
        mvc.perform(get("/api/miembros").param("estado", "SUSPENDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].id").value(4));

        mvc.perform(post("/api/planes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Starter\",\"includedHoursPerMonth\":10,\"overageDiscount\":0,\"maxActiveBookings\":5}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RN-11"));

        mvc.perform(delete("/api/sedes/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RN-12"));

        mvc.perform(delete("/api/salas/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RN-12"));
    }

    @Test
    void rejectsInvalidBookingDurationsAndOutsideLocationHours() throws Exception {
        LocalDate day = LocalDate.now().plusDays(6);
        LocalDateTime start = day.atTime(12, 0);

        postBooking(start, start.plusMinutes(45), 409)
                .andExpect(jsonPath("$.codigo").value("RN-02"));
        postBooking(day.atTime(7, 30), day.atTime(8, 30), 409)
                .andExpect(jsonPath("$.codigo").value("RN-03"));
        postBooking(start, start.plusHours(9), 409)
                .andExpect(jsonPath("$.codigo").value("RN-02"));
    }

    @Test
    void rejectsConfirmOfCancelledBookingAndServiceChangeAfterConfirmation() throws Exception {
        mvc.perform(post("/api/reservas/7/confirmar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RN-09"));

        mvc.perform(post("/api/reservas/3/servicios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"servicioId\":1,\"quantity\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RN-10"));
    }

    private void assertCount(String table, int expected) {
        Number count = (Number) entityManager.createNativeQuery("select count(*) from " + table)
                .getSingleResult();
        org.junit.jupiter.api.Assertions.assertEquals(expected, count.intValue(), "Filas inesperadas en " + table);
    }

    private org.springframework.test.web.servlet.ResultActions postBooking(LocalDateTime start, LocalDateTime end, int expectedStatus) throws Exception {
        String request = "{\"salaId\":1,\"miembroId\":1,\"startTime\":\"" + start + "\",\"endTime\":\"" + end
                + "\",\"attendees\":2,\"servicios\":[]}";
        return mvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().is(expectedStatus));
    }
}
