package com.nekoflow.backend.api.v1.admin.dto;

public record AdminSeekStreamingVideoResponse(
    String id,
    String name,
    String status,
    Integer width,
    Integer height,
    Long size,
    Integer duration,
    String poster,
    String preview,
    String folderId
) {
}
