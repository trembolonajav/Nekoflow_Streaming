package com.nekoflow.backend.api.v1.notification;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.nekoflow.backend.api.v1.common.dto.ApiMessageResponse;
import com.nekoflow.backend.api.v1.common.dto.ApiPageResponse;
import com.nekoflow.backend.api.v1.notification.dto.NotificationResponse;
import com.nekoflow.backend.api.v1.notification.dto.UnreadCountResponse;
import com.nekoflow.backend.domain.entity.NotificationEntity;
import com.nekoflow.backend.domain.entity.NotificationReadEntity;
import com.nekoflow.backend.domain.entity.UserEntity;
import com.nekoflow.backend.domain.enums.NotificationSeverity;
import com.nekoflow.backend.domain.enums.NotificationTargetType;
import com.nekoflow.backend.domain.enums.NotificationType;
import com.nekoflow.backend.domain.enums.RelatedEntityType;
import com.nekoflow.backend.domain.enums.RoleCode;
import com.nekoflow.backend.domain.repository.NotificationReadRepository;
import com.nekoflow.backend.domain.repository.NotificationRepository;
import com.nekoflow.backend.domain.repository.UserRepository;
import com.nekoflow.backend.security.AppUserPrincipal;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationReadRepository notificationReadRepository;
    private final UserRepository userRepository;

    public NotificationService(
        NotificationRepository notificationRepository,
        NotificationReadRepository notificationReadRepository,
        UserRepository userRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationReadRepository = notificationReadRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ApiPageResponse<NotificationResponse> list(int page, int size) {
        UserEntity user = currentUser();
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 50)));
        Page<NotificationEntity> notifications = notificationRepository.findVisibleForUser(user.getId(), roleCodes(user), pageable);
        Set<UUID> readIds = notificationReadRepository
            .findByNotificationIdInAndUserId(notifications.map(NotificationEntity::getId).toList(), user.getId())
            .stream()
            .map(read -> read.getNotification().getId())
            .collect(Collectors.toSet());

        List<NotificationResponse> items = notifications.stream()
            .map(notification -> toResponse(notification, readIds.contains(notification.getId())))
            .toList();

        return new ApiPageResponse<>(items, notifications.getTotalElements(), pageable.getPageNumber(), pageable.getPageSize());
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount() {
        UserEntity user = currentUser();
        return new UnreadCountResponse(notificationRepository.countUnreadForUser(user.getId(), roleCodes(user)));
    }

    @Transactional
    public ApiMessageResponse markRead(UUID notificationId) {
        UserEntity user = currentUser();
        NotificationEntity notification = notificationRepository.findById(notificationId)
            .filter(item -> canSee(item, user))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        notificationReadRepository.findByNotificationIdAndUserId(notification.getId(), user.getId())
            .orElseGet(() -> {
                NotificationReadEntity read = new NotificationReadEntity();
                read.setId(UUID.randomUUID());
                read.setNotification(notification);
                read.setUser(user);
                return notificationReadRepository.save(read);
            });

        return new ApiMessageResponse("Notificação marcada como lida.");
    }

    @Transactional
    public ApiMessageResponse markAllRead() {
        UserEntity user = currentUser();
        List<NotificationEntity> visible = notificationRepository
            .findVisibleForUser(user.getId(), roleCodes(user), PageRequest.of(0, 200))
            .getContent();
        Set<UUID> existingReads = notificationReadRepository
            .findByNotificationIdInAndUserId(visible.stream().map(NotificationEntity::getId).toList(), user.getId())
            .stream()
            .map(read -> read.getNotification().getId())
            .collect(Collectors.toSet());

        visible.stream()
            .filter(notification -> !existingReads.contains(notification.getId()))
            .forEach(notification -> {
                NotificationReadEntity read = new NotificationReadEntity();
                read.setId(UUID.randomUUID());
                read.setNotification(notification);
                read.setUser(user);
                notificationReadRepository.save(read);
            });

        return new ApiMessageResponse("Notificações marcadas como lidas.");
    }

    @Transactional
    public void notifyRole(
        RoleCode role,
        NotificationType type,
        NotificationSeverity severity,
        String title,
        String message,
        RelatedEntityType relatedEntityType,
        UUID relatedEntityId,
        String actionUrl
    ) {
        NotificationEntity notification = new NotificationEntity();
        notification.setId(UUID.randomUUID());
        notification.setTargetType(NotificationTargetType.ROLE);
        notification.setTargetRole(role);
        fill(notification, type, severity, title, message, relatedEntityType, relatedEntityId, actionUrl);
        notificationRepository.save(notification);
    }

    private void fill(
        NotificationEntity notification,
        NotificationType type,
        NotificationSeverity severity,
        String title,
        String message,
        RelatedEntityType relatedEntityType,
        UUID relatedEntityId,
        String actionUrl
    ) {
        notification.setType(type);
        notification.setSeverity(severity);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedEntityType(relatedEntityType);
        notification.setRelatedEntityId(relatedEntityId);
        notification.setActionUrl(actionUrl);
    }

    private NotificationResponse toResponse(NotificationEntity notification, boolean read) {
        return new NotificationResponse(
            notification.getId().toString(),
            notification.getTitle(),
            notification.getMessage(),
            notification.getType().name(),
            notification.getSeverity().name(),
            notification.getTargetType().name(),
            notification.getRelatedEntityType() != null ? notification.getRelatedEntityType().name() : null,
            notification.getRelatedEntityId() != null ? notification.getRelatedEntityId().toString() : null,
            notification.getActionUrl(),
            notification.getCreatedAt() != null ? notification.getCreatedAt().toString() : null,
            read
        );
    }

    private boolean canSee(NotificationEntity notification, UserEntity user) {
        if (notification.getTargetType() == NotificationTargetType.GLOBAL) {
            return true;
        }
        if (notification.getTargetType() == NotificationTargetType.USER) {
            return notification.getTargetUser() != null && notification.getTargetUser().getId().equals(user.getId());
        }
        if (notification.getTargetType() == NotificationTargetType.ROLE) {
            return roleCodes(user).contains(notification.getTargetRole());
        }
        return false;
    }

    private UserEntity currentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof AppUserPrincipal appUserPrincipal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return userRepository.findById(appUserPrincipal.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private Collection<RoleCode> roleCodes(UserEntity user) {
        return user.getRoles().stream()
            .map(role -> role.getCode())
            .toList();
    }
}
