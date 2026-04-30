package com.nekoflow.backend.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nekoflow.backend.domain.entity.HeroConfigEntity;

public interface HeroConfigRepository extends JpaRepository<HeroConfigEntity, UUID> {

    @EntityGraph(attributePaths = {"anime"})
    Optional<HeroConfigEntity> findFirstByActiveTrueOrderByIdAsc();

    @EntityGraph(attributePaths = {"anime"})
    java.util.List<HeroConfigEntity> findAllByActiveTrueOrderBySortOrderAscIdAsc();
}
