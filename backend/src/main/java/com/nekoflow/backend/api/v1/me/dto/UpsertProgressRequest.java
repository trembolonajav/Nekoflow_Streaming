package com.nekoflow.backend.api.v1.me.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpsertProgressRequest(
    @NotNull UUID animeId,
    @NotNull UUID episodeId,
    @NotNull @Min(0) Integer progressSeconds,
    @NotNull @Min(1) Integer durationSeconds
) {
}
