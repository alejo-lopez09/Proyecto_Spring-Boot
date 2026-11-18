package com.proyecto.spring.security;

import com.proyecto.spring.config.ApplicationSettings;
import com.proyecto.spring.domain.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final ApplicationSettings settings;
    private final Clock clock;
    public JwtService(ApplicationSettings settings, Clock clock) {
        this.settings = settings; this.clock = clock;
        this.key = Keys.hmacShaKeyFor(settings.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }
    public String issue(CoworkUserPrincipal principal) {
        Date issued = Date.from(clock.instant());
        Date expires = Date.from(clock.instant().plusSeconds(settings.getExpirationSeconds()));
        return Jwts.builder().subject(principal.getUsername()).claim("role", principal.getRole().name())
                .claim("memberId", principal.getMemberId()).issuedAt(issued).expiration(expires)
                .signWith(key).compact();
    }
    public CoworkUserPrincipal parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        Object member = claims.get("memberId");
        return new CoworkUserPrincipal(claims.getSubject(), "", Role.valueOf(claims.get("role", String.class)),
                member instanceof Number number ? number.longValue() : null);
    }
}
