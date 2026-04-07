package com.assessment.securitystarter.security;

import com.assessment.securitystarter.config.SecurityStarterProperties;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private static final String VALID_SECRET = "ThisIsAValidSecretKeyWithAtLeast32Characters!";
    private static final String DIFFERENT_SECRET = "AnotherValidSecretKeyWithAtLeast32Characters!";

    private JwtTokenService jwtTokenService;
    private SecurityStarterProperties properties;

    @BeforeEach
    void setUp() {
        properties = new SecurityStarterProperties();
        properties.setJwtSecret(VALID_SECRET);
        properties.setTokenExpirySeconds(3600);
        properties.setTokenIssuer("test-issuer");
        jwtTokenService = new JwtTokenService(properties);
    }

    @Test
    void generateToken_shouldCreateValidToken() {
        String token = jwtTokenService.generateToken(1L, "testuser", List.of("ROLE_USER"));

        assertThat(token).isNotBlank();
        assertThat(jwtTokenService.isValid(token)).isTrue();
    }

    @Test
    void toPrincipal_shouldExtractCorrectClaims() {
        Long userId = 42L;
        String username = "john";
        List<String> roles = List.of("ROLE_USER", "ROLE_ADMIN");

        String token = jwtTokenService.generateToken(userId, username, roles);
        JwtUserPrincipal principal = jwtTokenService.toPrincipal(token);

        assertThat(principal.userId()).isEqualTo(userId);
        assertThat(principal.username()).isEqualTo(username);
        assertThat(principal.roles()).containsExactlyInAnyOrderElementsOf(roles);
    }

    @Test
    void isValid_withMalformedToken_shouldThrowException() {
        String malformedToken = "not.a.valid.jwt.token";

        assertThatThrownBy(() -> jwtTokenService.isValid(malformedToken))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    void isValid_withWrongSignature_shouldThrowException() {
        String token = jwtTokenService.generateToken(1L, "user", List.of("ROLE_USER"));

        // Create a new service with different secret
        SecurityStarterProperties differentProps = new SecurityStarterProperties();
        differentProps.setJwtSecret(DIFFERENT_SECRET);
        differentProps.setTokenExpirySeconds(3600);
        JwtTokenService differentService = new JwtTokenService(differentProps);

        assertThatThrownBy(() -> differentService.isValid(token))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void isValid_withExpiredToken_shouldThrowException() {
        // Create service with 0 second expiry
        SecurityStarterProperties expiredProps = new SecurityStarterProperties();
        expiredProps.setJwtSecret(VALID_SECRET);
        expiredProps.setTokenExpirySeconds(0);
        expiredProps.setTokenIssuer("test-issuer");
        JwtTokenService expiredService = new JwtTokenService(expiredProps);

        String token = expiredService.generateToken(1L, "user", List.of("ROLE_USER"));

        // Token should be expired immediately
        assertThatThrownBy(() -> expiredService.isValid(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void generateToken_withShortSecret_shouldThrowException() {
        SecurityStarterProperties shortSecretProps = new SecurityStarterProperties();
        shortSecretProps.setJwtSecret("short");
        shortSecretProps.setTokenExpirySeconds(3600);
        JwtTokenService shortSecretService = new JwtTokenService(shortSecretProps);

        assertThatThrownBy(() -> shortSecretService.generateToken(1L, "user", List.of("ROLE_USER")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 32 characters");
    }

    @Test
    void generateToken_withNullSecret_shouldThrowException() {
        SecurityStarterProperties nullSecretProps = new SecurityStarterProperties();
        nullSecretProps.setJwtSecret(null);
        nullSecretProps.setTokenExpirySeconds(3600);
        JwtTokenService nullSecretService = new JwtTokenService(nullSecretProps);

        assertThatThrownBy(() -> nullSecretService.generateToken(1L, "user", List.of("ROLE_USER")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be set");
    }

    @Test
    void getTokenExpirySeconds_shouldReturnConfiguredValue() {
        assertThat(jwtTokenService.getTokenExpirySeconds()).isEqualTo(3600);
    }
}
