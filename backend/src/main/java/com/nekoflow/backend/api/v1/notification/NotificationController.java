package com.nekoflow.backend.api.v1.notification;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nekoflow.backend.api.v1.common.dto.ApiMessageResponse;
import com.nekoflow.backend.api.v1.common.dto.ApiPageResponse;
import com.nekoflow.backend.api.v1.notification.dto.NotificationResponse;
import com.nekoflow.backend.api.v1.notification.dto.UnreadCountResponse;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiPageResponse<NotificationResponse>> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(notificationService.list(page, size));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> unreadCount() {
        return ResponseEntity.ok(notificationService.unreadCount());
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiMessageResponse> markRead(@PathVariable UUID id) {
        return ResponseEntity.ok(notificationService.markRead(id));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiMessageResponse> markAllRead() {
        return ResponseEntity.ok(notificationService.markAllRead());
    }
}
