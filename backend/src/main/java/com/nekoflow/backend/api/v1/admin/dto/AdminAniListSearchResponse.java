package com.nekoflow.backend.api.v1.admin.dto;

import java.util.List;

public record AdminAniListSearchResponse(
    Long id,
    String titleRomaji,
    String titleEnglish,
    String titleNative,
    String format,
    String status,
    Integer episodes,
    Integer seasonYear,
    String season,
    String coverImage,
    String bannerImage,
    Integer averageScore,
    String description,
    List<String> genres,
    List<String> studios
) {
}
