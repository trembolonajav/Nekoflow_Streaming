package com.nekoflow.backend.api.v1.community.dto;

public record CommentAuthorResponse(
    String id,
    String name,
    String handle,
    String avatarUrl,
    String badge
) {
}
