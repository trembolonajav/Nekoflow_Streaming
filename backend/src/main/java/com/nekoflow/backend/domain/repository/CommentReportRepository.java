package com.nekoflow.backend.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nekoflow.backend.domain.entity.CommentReportEntity;

public interface CommentReportRepository extends JpaRepository<CommentReportEntity, UUID> {

    @EntityGraph(attributePaths = {"comment", "comment.user", "comment.episode", "comment.episode.anime"})
    List<CommentReportEntity> findAllByOrderByCreatedAtDesc();

    long countByStatus(String status);
}
