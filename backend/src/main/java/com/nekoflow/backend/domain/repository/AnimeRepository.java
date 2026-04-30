package com.nekoflow.backend.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nekoflow.backend.domain.entity.AnimeEntity;
import com.nekoflow.backend.domain.enums.VisibilityStatus;

public interface AnimeRepository extends JpaRepository<AnimeEntity, UUID> {

    @EntityGraph(attributePaths = {"episodes"})
    java.util.List<AnimeEntity> findAllByOrderByTitleDisplayAsc();

    @EntityGraph(attributePaths = {"episodes"})
    Optional<AnimeEntity> findBySlugAndVisibility(String slug, VisibilityStatus visibility);

    Optional<AnimeEntity> findBySlugIgnoreCase(String slug);

    Optional<AnimeEntity> findByAnilistId(Long anilistId);
}
