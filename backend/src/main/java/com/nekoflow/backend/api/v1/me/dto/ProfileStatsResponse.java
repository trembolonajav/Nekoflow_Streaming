package com.nekoflow.backend.api.v1.me.dto;

public record ProfileStatsResponse(
    int continueWatchingCount,
    int watchlistCount,
    int historyCount,
    int commentCount
) {
}
