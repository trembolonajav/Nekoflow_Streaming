package com.nekoflow.backend.api.v1.admin.dto;

public record AdminEpisodeResponse(
    String id,
    String animeId,
    String animeTitle,
    Integer number,
    String title,
    String summary,
    Integer durationSeconds,
    String thumbnailUrl,
    String previewUrl,
    String status,
    String scheduledFor,
    String provider,
    String externalVideoId,
    String embedUrl,
    String playerUrl,
    String updatedAt
) {
}
