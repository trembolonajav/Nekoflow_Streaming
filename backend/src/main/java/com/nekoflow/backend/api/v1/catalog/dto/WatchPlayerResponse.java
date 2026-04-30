package com.nekoflow.backend.api.v1.catalog.dto;

public record WatchPlayerResponse(
    String animeSlug,
    String animeTitle,
    String episodeId,
    Integer episodeNumber,
    String episodeTitle,
    String summary,
    String thumbnailUrl,
    String provider,
    String embedUrl,
    String playerUrl,
    Integer durationSeconds
) {
}
