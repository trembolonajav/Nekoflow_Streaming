package com.nekoflow.backend.api.v1.me.dto;

public record WatchlistItemResponse(
    String id,
    String animeId,
    String animeSlug,
    String title,
    String coverUrl,
    String status,
    String createdAt
) {
}
