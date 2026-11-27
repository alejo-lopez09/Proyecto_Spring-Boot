package com.proyecto.spring.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    GroupedOpenApi catalogsApi() {
        return GroupedOpenApi.builder().group("catalogos")
                .pathsToMatch("/api/sedes/**", "/api/salas/**", "/api/equipamientos/**",
                        "/api/servicios-adicionales/**", "/api/planes/**").build();
    }
    @Bean
    GroupedOpenApi membersApi() {
        return GroupedOpenApi.builder().group("miembros").pathsToMatch("/api/miembros/**").build();
    }
    @Bean
    GroupedOpenApi bookingsApi() {
        return GroupedOpenApi.builder().group("reservas").pathsToMatch("/api/reservas/**", "/api/disponibilidad").build();
    }
    @Bean
    OpenAPI coworkhubApi() {
        return new OpenAPI().info(new Info().title("CoworkHub API").version("1.0.0")
                .description("REST API pública para administrar una red de coworking."));
    }
}
