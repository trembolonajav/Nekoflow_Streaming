package com.nekoflow.backend.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nekoflow.backend.domain.entity.AnimeSuggestionEntity;

public interface AnimeSuggestionRepository extends JpaRepository<AnimeSuggestionEntity, UUID> {

    List<AnimeSuggestionEntity> findAllByOrderByVotesDescCreatedAtAsc();

    long countByStatus(String status);
}
