package com.nekoflow.backend.api.v1.catalog.dto;

import java.util.List;

public record AnimeDetailResponse(
    String id,
    String slug,
    Long anilistId,
    String titleDisplay,
    String titleRomaji,
    String titleNative,
    String titleEnglish,
    String synopsis,
    String type,
    String status,
    String visibility,
    String seasonLabel,
    Integer year,
    String coverUrl,
    String bannerUrl,
    String studio,
    List<String> genres,
    List<AnimeEpisodeSummaryResponse> episodes
) {
}
