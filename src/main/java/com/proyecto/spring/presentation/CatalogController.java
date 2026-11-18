package com.proyecto.spring.presentation;

import com.proyecto.spring.business.CatalogService;
import com.proyecto.spring.presentation.dto.ApiDtos.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Catálogos", description = "Sedes, salas, equipamiento, servicios adicionales y planes")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Falta un JWT válido", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Rol insuficiente", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Recurso inexistente", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Duplicado o recurso en uso", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class CatalogController {
    private final CatalogService catalog;
    public CatalogController(CatalogService catalog) { this.catalog = catalog; }

    @GetMapping("/sedes") public List<LocationResponse> locations() { return catalog.locations(); }
    @GetMapping("/sedes/{id}") public LocationResponse location(@PathVariable Long id) { return catalog.location(id); }
    @PostMapping("/sedes") @PreAuthorize("hasRole('ADMIN')") public ResponseEntity<LocationResponse> createLocation(@Valid @RequestBody LocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalog.saveLocation(null, request));
    }
    @PutMapping("/sedes/{id}") @PreAuthorize("hasRole('ADMIN')") public LocationResponse updateLocation(@PathVariable Long id, @Valid @RequestBody LocationRequest request) {
        return catalog.saveLocation(id, request);
    }
    @DeleteMapping("/sedes/{id}") @PreAuthorize("hasRole('ADMIN')") public ResponseEntity<Void> deleteLocation(@PathVariable Long id) {
        catalog.deleteLocation(id); return ResponseEntity.ok().build();
    }

    @GetMapping("/salas") public List<RoomResponse> rooms(@RequestParam(required = false) Long sedeId) { return catalog.rooms(sedeId); }
    @GetMapping("/salas/{id}") public RoomResponse room(@PathVariable Long id) { return catalog.room(id); }
    @PostMapping("/salas") @PreAuthorize("hasRole('ADMIN')") public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody RoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalog.saveRoom(null, request));
    }
    @PutMapping("/salas/{id}") @PreAuthorize("hasRole('ADMIN')") public RoomResponse updateRoom(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return catalog.saveRoom(id, request);
    }
    @DeleteMapping("/salas/{id}") @PreAuthorize("hasRole('ADMIN')") public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        catalog.deleteRoom(id); return ResponseEntity.ok().build();
    }

    @GetMapping("/equipamientos") public List<EquipmentResponse> equipment() { return catalog.equipment(); }
    @GetMapping("/equipamientos/{id}") public EquipmentResponse equipment(@PathVariable Long id) { return catalog.equipment(id); }
    @PostMapping("/equipamientos") @PreAuthorize("hasRole('ADMIN')") public ResponseEntity<EquipmentResponse> createEquipment(@Valid @RequestBody EquipmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalog.saveEquipment(null, request));
    }
    @PutMapping("/equipamientos/{id}") @PreAuthorize("hasRole('ADMIN')") public EquipmentResponse updateEquipment(@PathVariable Long id, @Valid @RequestBody EquipmentRequest request) {
        return catalog.saveEquipment(id, request);
    }
    @DeleteMapping("/equipamientos/{id}") @PreAuthorize("hasRole('ADMIN')") public ResponseEntity<Void> deleteEquipment(@PathVariable Long id) {
        catalog.deleteEquipment(id); return ResponseEntity.ok().build();
    }

    @GetMapping("/servicios-adicionales") public List<ServiceResponse> services() { return catalog.services(); }
    @GetMapping("/servicios-adicionales/{id}") public ServiceResponse service(@PathVariable Long id) { return catalog.service(id); }
    @PostMapping("/servicios-adicionales") @PreAuthorize("hasRole('ADMIN')") public ResponseEntity<ServiceResponse> createService(@Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalog.saveService(null, request));
    }
    @PutMapping("/servicios-adicionales/{id}") @PreAuthorize("hasRole('ADMIN')") public ServiceResponse updateService(@PathVariable Long id, @Valid @RequestBody ServiceRequest request) {
        return catalog.saveService(id, request);
    }
    @DeleteMapping("/servicios-adicionales/{id}") @PreAuthorize("hasRole('ADMIN')") public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        catalog.deleteService(id); return ResponseEntity.ok().build();
    }

    @GetMapping("/planes") public List<PlanResponse> plans() { return catalog.plans(); }
    @GetMapping("/planes/{id}") public PlanResponse plan(@PathVariable Long id) { return catalog.plan(id); }
    @PostMapping("/planes") @PreAuthorize("hasRole('ADMIN')") public ResponseEntity<PlanResponse> createPlan(@Valid @RequestBody PlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalog.savePlan(null, request));
    }
    @PutMapping("/planes/{id}") @PreAuthorize("hasRole('ADMIN')") public PlanResponse updatePlan(@PathVariable Long id, @Valid @RequestBody PlanRequest request) {
        return catalog.savePlan(id, request);
    }
    @DeleteMapping("/planes/{id}") @PreAuthorize("hasRole('ADMIN')") public ResponseEntity<Void> deletePlan(@PathVariable Long id) {
        catalog.deletePlan(id); return ResponseEntity.ok().build();
    }
}
