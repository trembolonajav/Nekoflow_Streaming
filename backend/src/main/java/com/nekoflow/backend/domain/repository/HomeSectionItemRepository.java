package com.nekoflow.backend.domain.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nekoflow.backend.domain.entity.HomeSectionEntity;
import com.nekoflow.backend.domain.entity.HomeSectionItemEntity;

public interface HomeSectionItemRepository extends JpaRepository<HomeSectionItemEntity, UUID> {

    void deleteBySection(HomeSectionEntity section);
}
