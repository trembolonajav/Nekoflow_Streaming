package com.nekoflow.backend.api.v1.community.dto;

import java.util.List;

public record CommentResponse(
    String id,
    String animeId,
    String episodeId,
    String body,
    boolean containsSpoiler,
    String createdAt,
    CommentAuthorResponse user,
    List<CommentResponse> replies
) {
}
