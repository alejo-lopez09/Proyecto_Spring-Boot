package com.proyecto.spring.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.spring.presentation.dto.ApiDtos.ErrorResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final ObjectMapper mapper;
    private final Clock clock;
    public JwtAuthenticationFilter(JwtService jwtService, ObjectMapper mapper, Clock clock) {
        this.jwtService = jwtService; this.mapper = mapper; this.clock = clock;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                CoworkUserPrincipal principal = jwtService.parse(header.substring(7));
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, header.substring(7), principal.getAuthorities()));
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                mapper.writeValue(response.getOutputStream(), new ErrorResponse("Token inválido o vencido", "UNAUTHORIZED", Instant.now(clock)));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
