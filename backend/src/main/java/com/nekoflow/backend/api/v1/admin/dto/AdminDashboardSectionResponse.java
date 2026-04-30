package com.nekoflow.backend.api.v1.admin.dto;

public record AdminDashboardSectionResponse(
    String id,
    String name,
    String mode,
    int items,
    boolean active
) {
}
