package com.nekoflow.backend.domain.repository;

import java.util.Collection;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nekoflow.backend.domain.entity.NotificationEntity;
import com.nekoflow.backend.domain.enums.RoleCode;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    @Query("""
        select n
        from NotificationEntity n
        where (n.expiresAt is null or n.expiresAt > current_timestamp)
          and (
            n.targetType = com.nekoflow.backend.domain.enums.NotificationTargetType.GLOBAL
            or (n.targetType = com.nekoflow.backend.domain.enums.NotificationTargetType.USER and n.targetUser.id = :userId)
            or (n.targetType = com.nekoflow.backend.domain.enums.NotificationTargetType.ROLE and n.targetRole in :roles)
          )
        order by n.createdAt desc
        """)
    Page<NotificationEntity> findVisibleForUser(
        @Param("userId") UUID userId,
        @Param("roles") Collection<RoleCode> roles,
        Pageable pageable
    );

    @Query("""
        select count(n)
        from NotificationEntity n
        where (n.expiresAt is null or n.expiresAt > current_timestamp)
          and (
            n.targetType = com.nekoflow.backend.domain.enums.NotificationTargetType.GLOBAL
            or (n.targetType = com.nekoflow.backend.domain.enums.NotificationTargetType.USER and n.targetUser.id = :userId)
            or (n.targetType = com.nekoflow.backend.domain.enums.NotificationTargetType.ROLE and n.targetRole in :roles)
          )
          and not exists (
            select r.id from NotificationReadEntity r
            where r.notification.id = n.id and r.user.id = :userId
          )
        """)
    long countUnreadForUser(@Param("userId") UUID userId, @Param("roles") Collection<RoleCode> roles);
}
