package com.assessment.securitystarter.security;

import com.assessment.securitystarter.config.SecurityStarterProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String VALID_SECRET = "ThisIsAValidSecretKeyWithAtLeast32Characters!";

    private JwtAuthenticationFilter filter;
    private JwtTokenService jwtTokenService;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        SecurityStarterProperties properties = new SecurityStarterProperties();
        properties.setJwtSecret(VALID_SECRET);
        properties.setTokenExpirySeconds(3600);
        properties.setTokenIssuer("test-issuer");
        jwtTokenService = new JwtTokenService(properties);
        filter = new JwtAuthenticationFilter(jwtTokenService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_withValidToken_shouldSetAuthentication() throws ServletException, IOException {
        String token = jwtTokenService.generateToken(1L, "testuser", List.of("ROLE_USER"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getPrincipal()).isInstanceOf(JwtUserPrincipal.class);

        JwtUserPrincipal principal = (JwtUserPrincipal) auth.getPrincipal();
        assertThat(principal.username()).isEqualTo("testuser");
        assertThat(principal.userId()).isEqualTo(1L);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_withoutAuthorizationHeader_shouldNotSetAuthentication() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_withInvalidToken_shouldClearContext() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid.token.here");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_withNonBearerToken_shouldNotSetAuthentication() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_withExpiredToken_shouldClearContext() throws ServletException, IOException {
        // Create service with 0 second expiry
        SecurityStarterProperties expiredProps = new SecurityStarterProperties();
        expiredProps.setJwtSecret(VALID_SECRET);
        expiredProps.setTokenExpirySeconds(0);
        JwtTokenService expiredService = new JwtTokenService(expiredProps);
        JwtAuthenticationFilter expiredFilter = new JwtAuthenticationFilter(expiredService);

        String token = expiredService.generateToken(1L, "user", List.of("ROLE_USER"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        expiredFilter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();

        verify(filterChain).doFilter(request, response);
    }
}
