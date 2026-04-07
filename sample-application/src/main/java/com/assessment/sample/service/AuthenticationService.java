package com.assessment.sample.service;

import com.assessment.sample.api.dto.LoginRequest;
import com.assessment.sample.api.dto.LoginResponse;
import com.assessment.sample.domain.entity.User;
import com.assessment.securitystarter.security.JwtTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtTokenService jwtTokenService;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            UserService userService,
            JwtTokenService jwtTokenService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
    }

    public LoginResponse authenticate(LoginRequest request) {
        log.debug("Processing login attempt");
        
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = userService.findByUsername(request.username());
        log.info("Authentication successful for user '{}'", request.username());
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName())
                .toList();

        String token = jwtTokenService.generateToken(user.getId(), user.getUsername(), roles);

        return new LoginResponse(
                token,
                "Bearer",
                jwtTokenService.getTokenExpirySeconds(),
                user.getId(),
                user.getUsername(),
                roles
        );
    }
}
