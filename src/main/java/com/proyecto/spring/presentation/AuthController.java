package com.proyecto.spring.presentation;

import com.proyecto.spring.config.ApplicationSettings;
import com.proyecto.spring.presentation.dto.ApiDtos.*;
import com.proyecto.spring.security.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión y perfil de la cuenta autenticada")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final ApplicationSettings settings;
    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService, ApplicationSettings settings) {
        this.authenticationManager = authenticationManager; this.jwtService = jwtService; this.settings = settings;
    }
    @PostMapping("/login") @Operation(summary = "Iniciar sesión y obtener JWT (validez de 60 minutos)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
                    examples = @ExampleObject(value = "{\"username\":\"ana\",\"password\":\"password\"}"))),
            responses = @ApiResponse(responseCode = "401", description = "Credenciales incorrectas",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))))
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        return new LoginResponse(jwtService.issue((CoworkUserPrincipal) authentication.getPrincipal()), "Bearer", settings.getExpirationSeconds());
    }
    @GetMapping("/perfil") @SecurityRequirement(name = "bearerAuth")
    public ProfileResponse profile(@AuthenticationPrincipal CoworkUserPrincipal principal) {
        return new ProfileResponse(principal.getUsername(), principal.getRole(), principal.getMemberId());
    }
}
