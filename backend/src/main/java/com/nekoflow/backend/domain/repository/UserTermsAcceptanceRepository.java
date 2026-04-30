package com.nekoflow.backend.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nekoflow.backend.domain.entity.UserTermsAcceptanceEntity;

public interface UserTermsAcceptanceRepository extends JpaRepository<UserTermsAcceptanceEntity, UUID> {

    Optional<UserTermsAcceptanceEntity> findTopByUserIdOrderByAcceptedAtDesc(UUID userId);
}
