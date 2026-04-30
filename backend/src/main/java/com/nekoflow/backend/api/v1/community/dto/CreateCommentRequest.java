package com.nekoflow.backend.api.v1.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
    @NotBlank
    @Size(max = 500)
    String body,
    Boolean containsSpoiler
) {
}
