package com.nekoflow.backend.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nekoflow.backend.domain.entity.WatchlistEntity;

public interface WatchlistRepository extends JpaRepository<WatchlistEntity, UUID> {

    @EntityGraph(attributePaths = {"anime"})
    List<WatchlistEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<WatchlistEntity> findByUserIdAndAnimeId(UUID userId, UUID animeId);

    long countByUserId(UUID userId);
}
