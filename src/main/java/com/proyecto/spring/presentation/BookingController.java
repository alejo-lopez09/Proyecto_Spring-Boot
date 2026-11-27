package com.proyecto.spring.presentation;

import com.proyecto.spring.business.BookingService;
import com.proyecto.spring.domain.BookingStatus;
import com.proyecto.spring.presentation.dto.ApiDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.media.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Reservas", description = "Disponibilidad, reservas, servicios asociados y transiciones")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Solicitud inválida", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Recurso inexistente", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Regla de negocio incumplida (RN-01 a RN-10)", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class BookingController {
    private final BookingService bookings;
    public BookingController(BookingService bookings) { this.bookings = bookings; }

    @GetMapping("/disponibilidad") @Operation(summary = "Buscar salas disponibles en una sede y horario")
    public List<AvailabilityResponse> availability(@RequestParam Long sedeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaFin,
            @RequestParam(required = false) Integer capacidadMinima,
            @RequestParam(required = false) List<Long> equipamientoIds) {
        return bookings.availability(sedeId, fecha, fecha.atTime(horaInicio), fecha.atTime(horaFin), capacidadMinima, equipamientoIds);
    }
    @PostMapping("/reservas")
    @Operation(summary = "Crear una reserva, calcular el precio de sala y congelar los precios de servicios",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    examples = @ExampleObject(value = "{\"salaId\":1,\"miembroId\":1,\"startTime\":\"2030-06-03T10:00:00\",\"endTime\":\"2030-06-03T11:00:00\",\"attendees\":3,\"servicios\":[{\"servicioId\":1,\"quantity\":2}]}"))))
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookings.create(request));
    }
    @GetMapping("/reservas/{id}")
    public BookingResponse get(@PathVariable Long id) { return bookings.get(id); }
    @GetMapping("/reservas")
    public List<BookingResponse> search(@RequestParam(required = false) Long miembroId, @RequestParam(required = false) Long salaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) BookingStatus estado) {
        return bookings.search(miembroId, salaId, fecha, estado);
    }
    @PostMapping("/reservas/{id}/servicios")
    public BookingResponse addService(@PathVariable Long id, @Valid @RequestBody BookingItemRequest request) { return bookings.addService(id, request); }
    @DeleteMapping("/reservas/{id}/servicios/{servicioId}")
    public BookingResponse removeService(@PathVariable Long id, @PathVariable Long servicioId) { return bookings.removeService(id, servicioId); }
    @PostMapping("/reservas/{id}/confirmar")
    public BookingResponse confirm(@PathVariable Long id) { return bookings.confirm(id); }
    @PostMapping("/reservas/{id}/cancelar") @Operation(summary = "Cancelar con cargo según la anticipación")
    public BookingResponse cancel(@PathVariable Long id) { return bookings.cancel(id); }
    @PostMapping("/reservas/{id}/completar")
    public BookingResponse complete(@PathVariable Long id) { return bookings.complete(id); }
}
