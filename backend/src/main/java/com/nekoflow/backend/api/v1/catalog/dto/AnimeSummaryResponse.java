package com.nekoflow.backend.api.v1.catalog.dto;

import java.util.List;

public record AnimeSummaryResponse(
    String id,
    String slug,
    Long anilistId,
    String titleDisplay,
    String titleRomaji,
    String synopsis,
    String coverUrl,
    String bannerUrl,
    String type,
    String status,
    String visibility,
    String seasonLabel,
    Integer year,
    String studio,
    Double averageScore,
    List<String> genres,
    Integer episodesCount
) {
}
