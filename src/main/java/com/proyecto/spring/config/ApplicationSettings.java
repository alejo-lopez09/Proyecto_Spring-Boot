package com.proyecto.spring.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApplicationSettings {
    @Value("${coworkhub.jwt.secret}")
    private String jwtSecret;
    @Value("${coworkhub.jwt.expiration-seconds:3600}")
    private long expirationSeconds;

    @PostConstruct
    void validate() {
        if (jwtSecret == null || jwtSecret.length() < 32) throw new IllegalStateException("coworkhub.jwt.secret must contain at least 32 characters");
        if (expirationSeconds != 3600) throw new IllegalStateException("JWT expiration must be 3600 seconds");
    }
    public String getJwtSecret() { return jwtSecret; }
    public long getExpirationSeconds() { return expirationSeconds; }
}
