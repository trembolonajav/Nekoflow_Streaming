package com.nekoflow.backend.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nekoflow.backend.domain.entity.WatchProgressEntity;

public interface WatchProgressRepository extends JpaRepository<WatchProgressEntity, UUID> {

    @EntityGraph(attributePaths = {"anime", "episode"})
    List<WatchProgressEntity> findTop12ByUserIdOrderByLastWatchedAtDesc(UUID userId);

    @EntityGraph(attributePaths = {"anime", "episode"})
    Optional<WatchProgressEntity> findByUserIdAndAnimeId(UUID userId, UUID animeId);
}
