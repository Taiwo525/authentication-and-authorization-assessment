package com.assessment.securitystarter.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
    }

    @Test
    void handleBadCredentials_shouldReturn401WithGenericMessage() {
        BadCredentialsException exception = new BadCredentialsException("Bad credentials");

        ResponseEntity<ApiErrorResponse> response = handler.handleBadCredentials(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ApiErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.status()).isEqualTo(401);
        assertThat(body.error()).isEqualTo("Unauthorized");
        assertThat(body.message()).isEqualTo("Invalid username or password");
        assertThat(body.path()).isEqualTo("/api/test");
    }

    @Test
    void handleAuthentication_shouldReturn401() {
        InsufficientAuthenticationException exception = new InsufficientAuthenticationException("Full authentication required");

        ResponseEntity<ApiErrorResponse> response = handler.handleAuthentication(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ApiErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.status()).isEqualTo(401);
        assertThat(body.message()).isEqualTo("Full authentication required");
    }

    @Test
    void handleAccessDenied_shouldReturn403() {
        AccessDeniedException exception = new AccessDeniedException("Access is denied");

        ResponseEntity<ApiErrorResponse> response = handler.handleAccessDenied(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ApiErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.status()).isEqualTo(403);
        assertThat(body.error()).isEqualTo("Forbidden");
        assertThat(body.message()).isEqualTo("Access denied");
    }

    @Test
    void handleValidation_shouldReturn400WithFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("loginRequest", "password", "Password is required");
        FieldError fieldError2 = new FieldError("loginRequest", "username", "Username is required");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ApiErrorResponse> response = handler.handleValidation(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.status()).isEqualTo(400);
        assertThat(body.error()).isEqualTo("Bad Request");
        assertThat(body.message()).isEqualTo("Password is required, Username is required");
    }

    @Test
    void handleIllegalArgument_shouldReturn400() {
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");

        ResponseEntity<ApiErrorResponse> response = handler.handleIllegalArgument(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.status()).isEqualTo(400);
        assertThat(body.message()).isEqualTo("Invalid argument");
    }

    @Test
    void handleGeneral_shouldReturn500() {
        Exception exception = new RuntimeException("Something went wrong");

        ResponseEntity<ApiErrorResponse> response = handler.handleGeneral(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ApiErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.status()).isEqualTo(500);
        assertThat(body.error()).isEqualTo("Internal Server Error");
        assertThat(body.message()).isEqualTo("Unexpected server error");
    }

    @Test
    void apiErrorResponse_shouldContainTimestamp() {
        BadCredentialsException exception = new BadCredentialsException("test");

        ResponseEntity<ApiErrorResponse> response = handler.handleBadCredentials(exception, request);

        ApiErrorResponse body = Objects.requireNonNull(response.getBody());
        assertThat(body.timestamp()).isNotNull();
    }
}
