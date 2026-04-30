package com.nekoflow.backend.api.v1.admin.dto;

public record AdminSuggestionResponse(
    String id,
    Integer rank,
    String title,
    Integer votes,
    String status,
    String note,
    String createdAt
) {
}
