package com.aurahealth.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.security.Key;
import java.util.Date;
import java.util.List;

/**
 * Lightweight JWT utility at the gateway layer.
 * Uses the same HMAC secret as the monolith's JwtTokenProvider
 * so tokens issued here are fully compatible with downstream services.
 */
@Component
public class GatewayJwtUtil {

    private final Key key;

    public GatewayJwtUtil(@Value("${aura.jwt-secret}") String jwtSecret) {
        if (jwtSecret.length() < 32) throw new IllegalStateException("AURA_JWT_SECRET must be at least 32 characters");
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    // 24-hour validity (matches monolith)
    private static final long VALIDITY_MS = 86_400_000;

    /**
     * Generate a signed JWT containing the user's email and roles.
     * This token is set as an HttpOnly cookie by the success handler
     * and validated by the monolith's JwtTokenFilter on downstream requests.
     */
    public String generateToken(String email, String name, List<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + VALIDITY_MS);

        return Jwts.builder()
                .setSubject(email)
                .claim("name", name)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
