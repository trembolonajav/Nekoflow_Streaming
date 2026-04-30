package com.nekoflow.backend.api.v1.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminEpisodeUpsertRequest(
    @NotBlank String animeId,
    @NotNull Integer number,
    @NotBlank String title,
    String summary,
    Integer durationSeconds,
    String thumbnailUrl,
    String previewUrl,
    @NotBlank String status,
    String scheduledFor,
    String provider,
    String externalVideoId,
    String embedUrl,
    String playerUrl
) {
}
