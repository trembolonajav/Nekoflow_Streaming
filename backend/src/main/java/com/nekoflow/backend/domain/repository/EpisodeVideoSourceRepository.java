package com.nekoflow.backend.domain.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nekoflow.backend.domain.entity.EpisodeEntity;
import com.nekoflow.backend.domain.entity.EpisodeVideoSourceEntity;

public interface EpisodeVideoSourceRepository extends JpaRepository<EpisodeVideoSourceEntity, UUID> {

    void deleteByEpisode(EpisodeEntity episode);
}
