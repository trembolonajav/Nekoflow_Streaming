package com.nekoflow.backend.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nekoflow.backend.domain.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    @EntityGraph(attributePaths = {"roles"})
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"roles"})
    Optional<UserEntity> findByGoogleSub(String googleSub);

    boolean existsByEmailIgnoreCase(String email);
}
