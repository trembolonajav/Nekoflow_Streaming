package com.nekoflow.backend.api.v1.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleAuthRequest(
    @NotBlank String idToken,
    boolean acceptTerms
) {
}
