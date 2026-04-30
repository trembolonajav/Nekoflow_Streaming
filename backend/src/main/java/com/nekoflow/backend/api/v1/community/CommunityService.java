package com.nekoflow.backend.api.v1.community;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nekoflow.backend.api.v1.community.dto.CommentAuthorResponse;
import com.nekoflow.backend.api.v1.community.dto.CommentResponse;
import com.nekoflow.backend.api.v1.community.dto.CreateCommentRequest;
import com.nekoflow.backend.domain.entity.CommentEntity;
import com.nekoflow.backend.domain.entity.EpisodeEntity;
import com.nekoflow.backend.domain.entity.UserEntity;
import com.nekoflow.backend.domain.repository.AnimeRepository;
import com.nekoflow.backend.domain.repository.CommentRepository;
import com.nekoflow.backend.domain.repository.EpisodeRepository;
import com.nekoflow.backend.domain.repository.UserRepository;
import com.nekoflow.backend.security.AppUserPrincipal;

@Service
public class CommunityService {

    private static final String VISIBLE = "VISIBLE";

    private final CommentRepository commentRepository;
    private final EpisodeRepository episodeRepository;
    private final AnimeRepository animeRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    public CommunityService(
        CommentRepository commentRepository,
        EpisodeRepository episodeRepository,
        AnimeRepository animeRepository,
        UserRepository userRepository,
        EntityManager entityManager
    ) {
        this.commentRepository = commentRepository;
        this.episodeRepository = episodeRepository;
        this.animeRepository = animeRepository;
        this.userRepository = userRepository;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listEpisodeComments(UUID episodeId) {
        if (!episodeRepository.existsById(episodeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Episode not found");
        }
        return buildThread(commentRepository.findByEpisodeIdAndStatusOrderByCreatedAtAsc(episodeId, VISIBLE));
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listAnimeCommentsBySlug(String slug) {
        UUID animeId = animeRepository.findBySlugIgnoreCase(slug)
            .map(com.nekoflow.backend.domain.entity.AnimeEntity::getId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anime not found"));
        return commentRepository.findByAnimeIdAndStatusAndParentIsNullOrderByCreatedAtDesc(animeId, VISIBLE).stream()
            .map(comment -> toResponse(comment, List.of()))
            .toList();
    }

    @Transactional
    public CommentResponse createEpisodeComment(UUID episodeId, CreateCommentRequest request) {
        EpisodeEntity episode = episodeRepository.findById(episodeId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Episode not found"));

        UserEntity user = currentUser();
        CommentEntity comment = new CommentEntity();
        comment.setId(UUID.randomUUID());
        comment.setAnime(episode.getAnime());
        comment.setEpisode(episode);
        comment.setUser(user);
        comment.setBody(request.body().trim());
        comment.setContainsSpoiler(Boolean.TRUE.equals(request.containsSpoiler()));
        comment.setStatus(VISIBLE);

        CommentEntity saved = commentRepository.saveAndFlush(comment);
        entityManager.refresh(saved);
        return toResponse(saved, List.of());
    }

    @Transactional
    public CommentResponse createReply(UUID parentId, CreateCommentRequest request) {
        CommentEntity parent = commentRepository.findByIdAndStatus(parentId, VISIBLE)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        UserEntity user = currentUser();
        CommentEntity reply = new CommentEntity();
        reply.setId(UUID.randomUUID());
        reply.setAnime(parent.getAnime());
        reply.setEpisode(parent.getEpisode());
        reply.setParent(parent);
        reply.setUser(user);
        reply.setBody(request.body().trim());
        reply.setContainsSpoiler(Boolean.TRUE.equals(request.containsSpoiler()));
        reply.setStatus(VISIBLE);

        CommentEntity saved = commentRepository.saveAndFlush(reply);
        entityManager.refresh(saved);
        return toResponse(saved, List.of());
    }

    private UserEntity currentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof AppUserPrincipal appUserPrincipal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return userRepository.findById(appUserPrincipal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private List<CommentResponse> buildThread(List<CommentEntity> comments) {
        Map<UUID, List<CommentEntity>> repliesByParent = comments.stream()
            .filter(comment -> comment.getParent() != null)
            .collect(Collectors.groupingBy(comment -> comment.getParent().getId()));

        return comments.stream()
            .filter(comment -> comment.getParent() == null)
            .map(comment -> toResponse(comment, repliesByParent.getOrDefault(comment.getId(), List.of()).stream()
                .map(reply -> toResponse(reply, List.of()))
                .toList()))
            .toList();
    }

    private CommentResponse toResponse(CommentEntity comment, List<CommentResponse> replies) {
        return new CommentResponse(
            comment.getId().toString(),
            comment.getAnime().getId().toString(),
            comment.getEpisode().getId().toString(),
            comment.getBody(),
            comment.isContainsSpoiler(),
            comment.getCreatedAt() != null ? comment.getCreatedAt().toString() : null,
            toAuthor(comment.getUser()),
            new ArrayList<>(replies)
        );
    }

    private CommentAuthorResponse toAuthor(UserEntity user) {
        String handle = user.getEmail().split("@")[0].toLowerCase();
        String badge = user.getRoles().stream()
            .map(role -> role.getCode().name())
            .filter(code -> !"USER".equals(code))
            .findFirst()
            .map(code -> switch (code) {
                case "ADMIN" -> "fundador";
                case "EDITOR", "MODERATOR" -> "curador";
                default -> null;
            })
            .orElse(null);

        return new CommentAuthorResponse(
            user.getId().toString(),
            user.getName(),
            handle,
            user.getAvatarUrl(),
            badge
        );
    }
}
