package com.nekoflow.backend.api.v1.admin.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminHomeSectionRequest(
    @NotBlank String code,
    @NotBlank String mode,
    boolean active,
    @NotNull Integer sortOrder,
    @NotNull List<String> manualItemIds
) {
}
