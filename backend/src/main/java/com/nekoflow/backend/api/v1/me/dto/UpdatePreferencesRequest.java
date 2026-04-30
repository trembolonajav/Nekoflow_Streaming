package com.nekoflow.backend.api.v1.me.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdatePreferencesRequest(
    boolean autoplay,
    boolean autoNext,
    @NotBlank String preferredAudio,
    @NotBlank String preferredSubtitle,
    @NotBlank String preferredQuality,
    boolean notifyReleases,
    boolean notifyNewEpisodes,
    boolean notifyWatchlist,
    boolean notifyMarketing
) {
}
