package com.nekoflow.backend.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nekoflow.backend.domain.entity.RefreshTokenEntity;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    @EntityGraph(attributePaths = {"user", "user.roles"})
    Optional<RefreshTokenEntity> findByToken(String token);

    @EntityGraph(attributePaths = {"user"})
    java.util.List<RefreshTokenEntity> findTop10ByUserIdOrderByCreatedAtDesc(UUID userId);
}
