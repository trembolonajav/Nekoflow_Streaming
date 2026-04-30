package com.nekoflow.backend.api.v1.me;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nekoflow.backend.api.v1.common.dto.ApiMessageResponse;
import com.nekoflow.backend.api.v1.me.dto.HistoryItemResponse;
import com.nekoflow.backend.api.v1.me.dto.ProfileResponse;
import com.nekoflow.backend.api.v1.me.dto.UpdatePreferencesRequest;
import com.nekoflow.backend.api.v1.me.dto.UpsertProgressRequest;
import com.nekoflow.backend.api.v1.me.dto.UserPreferencesResponse;
import com.nekoflow.backend.api.v1.me.dto.WatchlistItemResponse;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/v1/me")
public class MeController {

    private final MeService meService;

    public MeController(MeService meService) {
        this.meService = meService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> profile() {
        return ResponseEntity.ok(meService.getProfile());
    }

    @GetMapping("/watchlist")
    public ResponseEntity<List<WatchlistItemResponse>> watchlist() {
        return ResponseEntity.ok(meService.getWatchlist());
    }

    @PostMapping("/watchlist/{animeId}")
    public ResponseEntity<WatchlistItemResponse> addWatchlist(@PathVariable UUID animeId) {
        return ResponseEntity.ok(meService.addToWatchlist(animeId));
    }

    @DeleteMapping("/watchlist/{animeId}")
    public ResponseEntity<ApiMessageResponse> removeWatchlist(@PathVariable UUID animeId) {
        return ResponseEntity.ok(meService.removeFromWatchlist(animeId));
    }

    @GetMapping("/history")
    public ResponseEntity<List<HistoryItemResponse>> history() {
        return ResponseEntity.ok(meService.getHistory());
    }

    @DeleteMapping("/history/{historyId}")
    public ResponseEntity<ApiMessageResponse> deleteHistory(@PathVariable UUID historyId) {
        return ResponseEntity.ok(meService.deleteHistoryItem(historyId));
    }

    @DeleteMapping("/history")
    public ResponseEntity<ApiMessageResponse> clearHistory() {
        return ResponseEntity.ok(meService.clearHistory());
    }

    @GetMapping("/preferences")
    public ResponseEntity<UserPreferencesResponse> preferences() {
        return ResponseEntity.ok(meService.getPreferences());
    }

    @PutMapping("/preferences")
    public ResponseEntity<UserPreferencesResponse> updatePreferences(@Valid @RequestBody UpdatePreferencesRequest request) {
        return ResponseEntity.ok(meService.updatePreferences(request));
    }

    @PutMapping("/progress")
    public ResponseEntity<ApiMessageResponse> upsertProgress(@Valid @RequestBody UpsertProgressRequest request) {
        return ResponseEntity.ok(meService.upsertProgress(request));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiMessageResponse> revokeSession(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(meService.revokeSession(sessionId));
    }
}
