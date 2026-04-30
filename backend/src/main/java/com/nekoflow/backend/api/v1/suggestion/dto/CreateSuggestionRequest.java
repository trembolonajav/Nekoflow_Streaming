package com.nekoflow.backend.api.v1.suggestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSuggestionRequest(
    @NotBlank @Size(max = 255) String title,
    @Size(max = 1200) String note
) {
}
