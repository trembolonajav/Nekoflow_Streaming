package com.nekoflow.backend.api.v1.calendar;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sincronizacao e curadoria do calendario (protegido por /api/v1/admin/**). */
@RestController
@RequestMapping("/api/v1/admin/calendar")
public class AdminCalendarController {

    private final CalendarSyncService calendarSyncService;

    public AdminCalendarController(CalendarSyncService calendarSyncService) {
        this.calendarSyncService = calendarSyncService;
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> sync() {
        return ResponseEntity.ok(calendarSyncService.sync());
    }

    @PatchMapping("/anime/{animeId}")
    public ResponseEntity<Map<String, Object>> setVisibility(
        @PathVariable UUID animeId,
        @RequestBody Map<String, Object> body
    ) {
        boolean show = Boolean.TRUE.equals(body.get("show_in_calendar"));
        return ResponseEntity.ok(calendarSyncService.setAnimeVisibility(animeId, show));
    }
}
