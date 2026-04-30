package com.nekoflow.backend.api.v1.notification.dto;

public record NotificationResponse(
    String id,
    String title,
    String message,
    String type,
    String severity,
    String targetType,
    String relatedEntityType,
    String relatedEntityId,
    String actionUrl,
    String createdAt,
    boolean read
) {
}
