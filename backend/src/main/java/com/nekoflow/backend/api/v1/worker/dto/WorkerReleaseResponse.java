package com.nekoflow.backend.api.v1.worker.dto;

public record WorkerReleaseResponse(
    boolean ok,
    String message,
    String animeId,
    String episodeId,
    boolean animeCreated,
    boolean episodeCreated
) {
}
