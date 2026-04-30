package com.nekoflow.backend.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nekoflow.backend.domain.entity.HomeSectionEntity;

public interface HomeSectionRepository extends JpaRepository<HomeSectionEntity, UUID> {

    @EntityGraph(attributePaths = {"items", "items.anime", "items.episode", "items.episode.anime"})
    List<HomeSectionEntity> findByActiveTrueOrderBySortOrderAsc();

    @EntityGraph(attributePaths = {"items", "items.anime", "items.episode", "items.episode.anime"})
    List<HomeSectionEntity> findAllByOrderBySortOrderAsc();

    java.util.Optional<HomeSectionEntity> findByCode(String code);
}
