package com.nekoflow.backend.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nekoflow.backend.domain.entity.CommentEntity;

public interface CommentRepository extends JpaRepository<CommentEntity, UUID> {

    @EntityGraph(attributePaths = {"user", "parent"})
    List<CommentEntity> findByEpisodeIdAndStatusOrderByCreatedAtAsc(UUID episodeId, String status);

    @EntityGraph(attributePaths = {"user", "parent"})
    List<CommentEntity> findByAnimeIdAndStatusAndParentIsNullOrderByCreatedAtDesc(UUID animeId, String status);

    @EntityGraph(attributePaths = {"user", "episode", "anime", "parent"})
    Optional<CommentEntity> findByIdAndStatus(UUID id, String status);

    @EntityGraph(attributePaths = {"anime", "episode"})
    List<CommentEntity> findTop10ByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status);

    long countByUserIdAndStatus(UUID userId, String status);
}
