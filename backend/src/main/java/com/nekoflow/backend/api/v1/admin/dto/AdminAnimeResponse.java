package com.nekoflow.backend.api.v1.admin.dto;

import java.util.List;

public record AdminAnimeResponse(
    String id,
    Long anilistId,
    String slug,
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
    Integer episodesCount,
    String updatedAt,
    Boolean showInCalendar
) {
}
