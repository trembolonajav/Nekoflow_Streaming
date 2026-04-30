package com.nekoflow.backend.api.v1.calendar.dto;

import java.util.List;

public record CalendarDayResponse(
    Integer index,
    String label,
    String shortLabel,
    String dateIso,
    boolean isToday,
    List<CalendarReleaseResponse> releases
) {
}
