package com.proyecto.spring.presentation;

import com.proyecto.spring.business.MemberService;
import com.proyecto.spring.domain.MemberStatus;
import com.proyecto.spring.presentation.dto.ApiDtos.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.media.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.Clock;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/miembros")
@Tag(name = "Miembros", description = "Gestión de miembros y consulta de consumo mensual")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Miembro inexistente", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Documento o email duplicado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class MemberController {
    private final MemberService members;
    private final Clock clock;
    public MemberController(MemberService members, Clock clock) { this.members = members; this.clock = clock; }

    @GetMapping
    public List<MemberResponse> list(@RequestParam(required = false) String nombre, @RequestParam(required = false) MemberStatus estado) {
        return members.list(nombre, estado);
    }
    @GetMapping("/{id}")
    public MemberResponse get(@PathVariable Long id) { return members.get(id); }
    @PostMapping
    public ResponseEntity<MemberResponse> create(@Valid @RequestBody MemberCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(members.create(request));
    }
    @PutMapping("/{id}")
    public MemberResponse update(@PathVariable Long id, @Valid @RequestBody MemberUpdateRequest request) { return members.update(id, request); }
    @PatchMapping("/{id}/suspender")
    public MemberResponse suspend(@PathVariable Long id) { return members.suspend(id); }
    @PatchMapping("/{id}/activar")
    public MemberResponse activate(@PathVariable Long id) { return members.activate(id); }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { members.delete(id); return ResponseEntity.ok().build(); }
    @GetMapping("/{id}/consumo")
    public ConsumptionResponse consumption(@PathVariable Long id, @RequestParam(required = false) YearMonth mes) {
        return members.consumption(id, mes == null ? YearMonth.now(clock) : mes);
    }
}
