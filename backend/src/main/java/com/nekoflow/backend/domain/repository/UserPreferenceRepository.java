package com.nekoflow.backend.domain.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nekoflow.backend.domain.entity.UserPreferenceEntity;

public interface UserPreferenceRepository extends JpaRepository<UserPreferenceEntity, UUID> {
}
