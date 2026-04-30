package com.nekoflow.backend.api.v1.me.dto;

import java.util.List;

public record ProfileResponse(
    String id,
    String name,
    String email,
    String avatarUrl,
    ProfileStatsResponse stats,
    List<ContinueWatchingItemResponse> continueWatching
) {
}
