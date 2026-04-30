package com.nekoflow.backend.api.v1.admin.dto;

public record AdminDashboardHealthResponse(
    String id,
    String title,
    String description,
    int count,
    String severity
) {
}
