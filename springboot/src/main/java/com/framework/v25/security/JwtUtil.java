package com.framework.v25.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private final String secret;
    private final long   expirationMs;

    /**
     * FIXED: @Value injected via constructor parameter — not field injection.
     * Field-level @Value with @RequiredArgsConstructor causes null at runtime.
     */
    @Autowired
    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.secret       = secret;
        this.expirationMs = expirationMs;
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Issues a JWT that both Spring Boot and PostgREST can verify.
     * Embeds user_id so PostgreSQL RLS function current_user_id() works.
     */
    public String generateToken(UUID userId, String email) {
        return Jwts.builder()
                .subject(email)
                .claim("user_id", userId.toString())
                .claim("role", "authenticated")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key())
                .compact();
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public UUID extractUserId(String token) {
        String raw = (String) getClaims(token).get("user_id");
        return UUID.fromString(raw);
    }

    public boolean isValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}