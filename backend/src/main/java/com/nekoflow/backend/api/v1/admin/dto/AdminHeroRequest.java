package com.nekoflow.backend.api.v1.admin.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record AdminHeroRequest(
    @NotNull List<String> animeIds,
    String tag,
    String ctaLabel
) {
}
