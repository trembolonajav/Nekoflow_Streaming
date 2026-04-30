package com.nekoflow.backend.api.v1.admin.dto;

public record AdminDashboardPublicationResponse(
    String id,
    String title,
    String subtitle,
    String type,
    String status,
    String updatedAt,
    String thumb
) {
}
