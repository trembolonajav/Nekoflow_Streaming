package com.nekoflow.backend.api.v1.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(max = 120) String name,
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8, max = 120) String password,
    @NotBlank String confirmPassword,
    boolean acceptTerms
) {
}
