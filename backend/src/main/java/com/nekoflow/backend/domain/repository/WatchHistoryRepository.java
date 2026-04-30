package com.nekoflow.backend.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nekoflow.backend.domain.entity.WatchHistoryEntity;

public interface WatchHistoryRepository extends JpaRepository<WatchHistoryEntity, UUID> {

    @EntityGraph(attributePaths = {"anime", "episode"})
    List<WatchHistoryEntity> findTop20ByUserIdOrderByWatchedAtDesc(UUID userId);

    java.util.Optional<WatchHistoryEntity> findTop1ByUserIdAndEpisodeIdOrderByWatchedAtDesc(UUID userId, UUID episodeId);

    long countByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
