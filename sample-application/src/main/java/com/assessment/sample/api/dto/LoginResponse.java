package com.assessment.sample.api.dto;

import java.util.List;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        Long userId,
        String username,
        List<String> roles
) {
}
