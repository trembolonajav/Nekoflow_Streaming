package com.nekoflow.backend.api.v1.admin.dto;

public record AdminDashboardMetricResponse(
    String key,
    String label,
    String value,
    String delta,
    String trend
) {
}
