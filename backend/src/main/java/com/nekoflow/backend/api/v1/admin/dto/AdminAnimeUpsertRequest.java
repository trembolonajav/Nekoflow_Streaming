package com.nekoflow.backend.api.v1.admin.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminAnimeUpsertRequest(
    Long anilistId,
    @NotBlank String slug,
    @NotBlank String titleDisplay,
    String titleRomaji,
    String titleNative,
    String titleEnglish,
    String synopsis,
    @NotBlank String type,
    @NotBlank String status,
    @NotBlank String visibility,
    String seasonLabel,
    Integer year,
    String coverUrl,
    String bannerUrl,
    String studio,
    @NotNull List<String> genres
) {
}
