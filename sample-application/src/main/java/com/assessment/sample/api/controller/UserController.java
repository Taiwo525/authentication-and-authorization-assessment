package com.assessment.sample.api.controller;

import com.assessment.sample.api.dto.UserResponse;
import com.assessment.securitystarter.security.JwtUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(new UserResponse(
                principal.userId(),
                principal.username(),
                principal.roles()
        ));
    }
}
