package com.nekoflow.backend.api.v1.calendar.dto;

public record CalendarReleaseResponse(
    String id,
    String slug,
    String animeTitle,
    Integer episodeNumber,
    String thumbnail,
    String poster,
    String synopsisShort,
    java.util.List<String> genres,
    String time,
    String airDateIso,
    String language,
    String status,
    String studio
) {
}
