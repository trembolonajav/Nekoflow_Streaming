package com.nekoflow.backend.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nekoflow.backend.domain.entity.NotificationReadEntity;

public interface NotificationReadRepository extends JpaRepository<NotificationReadEntity, UUID> {

    Optional<NotificationReadEntity> findByNotificationIdAndUserId(UUID notificationId, UUID userId);

    List<NotificationReadEntity> findByNotificationIdInAndUserId(Collection<UUID> notificationIds, UUID userId);
}
