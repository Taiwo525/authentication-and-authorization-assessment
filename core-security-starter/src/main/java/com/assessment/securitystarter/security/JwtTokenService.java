package com.assessment.securitystarter.security;

import com.assessment.securitystarter.config.SecurityStarterProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class JwtTokenService {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);

    private final SecurityStarterProperties properties;

    public JwtTokenService(SecurityStarterProperties properties) {
        this.properties = properties;
    }

    public String generateToken(Long userId, String username, List<String> roles) {
        log.debug("Generating token for user '{}'", username);
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.getTokenExpirySeconds());
        return Jwts.builder()
                .subject(username)
                .issuer(properties.getTokenIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claims(Map.of(
                        "userId", userId,
                        "username", username,
                        "roles", roles
                ))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean isValid(String token) {
        parseClaims(token);
        return true;
    }

    public JwtUserPrincipal toPrincipal(String token) {
        Claims claims = parseClaims(token);
        Object userIdValue = claims.get("userId");
        Long userId;
        if (userIdValue instanceof Number number) {
            userId = number.longValue();
        } else if (userIdValue instanceof String value) {
            userId = Long.parseLong(value);
        } else {
            throw new IllegalArgumentException("Invalid user id in token");
        }
        String username = claims.get("username", String.class);
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles", List.class);
        return new JwtUserPrincipal(userId, username, roles);
    }

    public long getTokenExpirySeconds() {
        return properties.getTokenExpirySeconds();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        if (!StringUtils.hasText(properties.getJwtSecret())) {
            throw new IllegalArgumentException("security.starter.jwt-secret must be set");
        }
        byte[] keyBytes = properties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("security.starter.jwt-secret must be at least 32 characters");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
