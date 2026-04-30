package com.nekoflow.backend.api.v1.me.dto;

public record UserPreferencesResponse(
    boolean autoplay,
    boolean autoNext,
    String preferredAudio,
    String preferredSubtitle,
    String preferredQuality,
    boolean notifyReleases,
    boolean notifyNewEpisodes,
    boolean notifyWatchlist,
    boolean notifyMarketing
) {
}
