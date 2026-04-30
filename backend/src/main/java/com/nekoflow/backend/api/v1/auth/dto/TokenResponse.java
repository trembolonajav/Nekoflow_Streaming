package com.nekoflow.backend.api.v1.auth.dto;

import java.util.List;

public record TokenResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    String userId,
    String name,
    String email,
    List<String> roles,
    String provider
) {
}
