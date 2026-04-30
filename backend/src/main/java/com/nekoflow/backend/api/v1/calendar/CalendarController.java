package com.nekoflow.backend.api.v1.calendar;

import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nekoflow.backend.api.v1.calendar.dto.CalendarWeekResponse;

@RestController
@RequestMapping("/api/v1/calendar")
public class CalendarController {

    private final CalendarQueryService calendarQueryService;

    public CalendarController(CalendarQueryService calendarQueryService) {
        this.calendarQueryService = calendarQueryService;
    }

    @GetMapping
    public ResponseEntity<CalendarWeekResponse> week(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart
    ) {
        LocalDate reference = weekStart != null ? weekStart : LocalDate.now(ZoneOffset.UTC);
        return ResponseEntity.ok(calendarQueryService.getWeek(reference));
    }
}
