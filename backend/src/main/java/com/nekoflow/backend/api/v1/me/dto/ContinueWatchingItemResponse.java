package com.nekoflow.backend.api.v1.me.dto;

public record ContinueWatchingItemResponse(
    String animeId,
    String animeSlug,
    String animeTitle,
    String episodeId,
    Integer episodeNumber,
    String episodeTitle,
    String thumbnailUrl,
    Integer progressSeconds,
    Double progressPercent,
    Integer remainingMinutes
) {
}
