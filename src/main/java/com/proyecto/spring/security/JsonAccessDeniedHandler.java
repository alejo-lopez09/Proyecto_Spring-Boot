package com.proyecto.spring.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.spring.presentation.dto.ApiDtos.ErrorResponse;
import jakarta.servlet.http.*;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper mapper;
    private final Clock clock;
    public JsonAccessDeniedHandler(ObjectMapper mapper, Clock clock) { this.mapper = mapper; this.clock = clock; }
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), new ErrorResponse("No tiene permiso para esta operación", "FORBIDDEN", Instant.now(clock)));
    }
}
