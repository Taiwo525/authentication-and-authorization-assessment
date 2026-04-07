package com.assessment.securitystarter.security;

import java.util.List;

public record JwtUserPrincipal(
        Long userId,
        String username,
        List<String> roles
) {
}
