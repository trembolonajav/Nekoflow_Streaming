package com.nekoflow.backend.api.v1.catalog.dto;

public record AnimeSummaryResponse(
    String id,
    String slug,
    Long anilistId,
    String titleDisplay,
    String titleRomaji,
    String coverUrl,
    String bannerUrl,
    String type,
    String status,
    String visibility,
    Integer year
) {
}
