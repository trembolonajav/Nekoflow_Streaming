package com.nekoflow.backend.api.v1.catalog.dto;

public record HomeSectionItemResponse(
    String id,
    String animeId,
    String episodeId,
    String title,
    String subtitle,
    String coverUrl,
    String bannerUrl,
    String previewUrl,
    String slug
) {
}
