package com.nekoflow.backend.api.v1.admin.dto;

public record AdminCommentReportResponse(
    String id,
    String commentId,
    String user,
    String userInitial,
    String context,
    String reason,
    String body,
    Integer reportCount,
    String createdAt,
    String status
) {
}
