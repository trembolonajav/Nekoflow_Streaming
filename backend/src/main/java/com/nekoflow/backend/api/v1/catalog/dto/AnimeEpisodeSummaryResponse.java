package com.nekoflow.backend.api.v1.catalog.dto;

public record AnimeEpisodeSummaryResponse(
    String id,
    Integer number,
    String title,
    String status,
    String thumbnailUrl,
    Integer durationSeconds
) {
}
