package com.nekoflow.backend.api.v1.catalog.dto;

import java.util.List;

public record HomeSectionItemResponse(
    String id,
    String animeId,
    String episodeId,
    String title,
    String subtitle,
    String coverUrl,
    String bannerUrl,
    String previewUrl,
    String slug,
    String synopsis,
    String type,
    String status,
    String seasonLabel,
    Integer year,
    String studio,
    Double averageScore,
    List<String> genres
) {
}
