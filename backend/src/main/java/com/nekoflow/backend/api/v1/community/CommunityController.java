package com.nekoflow.backend.api.v1.community;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nekoflow.backend.api.v1.community.dto.CommentResponse;
import com.nekoflow.backend.api.v1.community.dto.CreateCommentRequest;

import jakarta.validation.Valid;

@RestController
@Validated
@RequestMapping("/api/v1")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping("/episodes/{episodeId}/comments")
    public ResponseEntity<List<CommentResponse>> episodeComments(@PathVariable UUID episodeId) {
        return ResponseEntity.ok(communityService.listEpisodeComments(episodeId));
    }

    @GetMapping("/animes/{slug}/comments")
    public ResponseEntity<List<CommentResponse>> animeComments(@PathVariable String slug) {
        return ResponseEntity.ok(communityService.listAnimeCommentsBySlug(slug));
    }

    @PostMapping("/episodes/{episodeId}/comments")
    public ResponseEntity<CommentResponse> createEpisodeComment(
        @PathVariable UUID episodeId,
        @Valid @RequestBody CreateCommentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(communityService.createEpisodeComment(episodeId, request));
    }

    @PostMapping("/comments/{commentId}/replies")
    public ResponseEntity<CommentResponse> createReply(
        @PathVariable UUID commentId,
        @Valid @RequestBody CreateCommentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(communityService.createReply(commentId, request));
    }
}
