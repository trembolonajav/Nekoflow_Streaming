package com.nekoflow.backend.api.v1.admin.dto;

public record AdminSeekStreamingFolderResponse(
    String id,
    String name,
    String parentId,
    Integer folderCount,
    Integer videoCount
) {
}
