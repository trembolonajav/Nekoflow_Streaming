package com.nekoflow.backend.api.v1.me.dto;

public record HistoryItemResponse(
    String id,
    String animeId,
    String animeSlug,
    String animeTitle,
    String episodeId,
    Integer episodeNumber,
    String episodeTitle,
    String thumbnailUrl,
    String watchedAt
) {
}
