package com.nekoflow.backend.api.v1.calendar.dto;

import java.util.List;

public record CalendarWeekResponse(
    String weekStartIso,
    String weekEndIso,
    String rangeLabel,
    String season,
    Integer year,
    List<CalendarDayResponse> days,
    Integer totalReleases
) {
}
