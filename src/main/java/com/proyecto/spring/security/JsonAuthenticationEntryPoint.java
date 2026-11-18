package com.proyecto.spring.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.spring.presentation.dto.ApiDtos.ErrorResponse;
import jakarta.servlet.http.*;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper mapper;
    private final Clock clock;
    public JsonAuthenticationEntryPoint(ObjectMapper mapper, Clock clock) { this.mapper = mapper; this.clock = clock; }
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), new ErrorResponse("Se requiere autenticación", "UNAUTHORIZED", Instant.now(clock)));
    }
}
