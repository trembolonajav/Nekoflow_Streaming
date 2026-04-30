package com.nekoflow.backend.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nekoflow.backend.domain.entity.EpisodeEntity;
import com.nekoflow.backend.domain.enums.EpisodeStatus;

public interface EpisodeRepository extends JpaRepository<EpisodeEntity, UUID> {

    @EntityGraph(attributePaths = {"anime", "videoSources"})
    java.util.List<EpisodeEntity> findAllByOrderByPublishedAtDescNumberDesc();

    @EntityGraph(attributePaths = {"anime", "videoSources"})
    Optional<EpisodeEntity> findByAnimeSlugAndNumberAndStatus(String animeSlug, Integer number, EpisodeStatus status);

    boolean existsById(UUID id);

    boolean existsByAnimeIdAndNumber(UUID animeId, Integer number);

    boolean existsByAnimeIdAndNumberAndIdNot(UUID animeId, Integer number, UUID id);

    @EntityGraph(attributePaths = {"anime", "videoSources"})
    Optional<EpisodeEntity> findByAnimeIdAndNumber(UUID animeId, Integer number);

    @EntityGraph(attributePaths = {"anime", "videoSources"})
    java.util.List<EpisodeEntity> findAllByStatusInOrderByScheduledForAscPublishedAtAscNumberAsc(java.util.Collection<EpisodeStatus> statuses);
}
