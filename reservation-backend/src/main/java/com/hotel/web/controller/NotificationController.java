package com.hotel.web.controller;

import com.hotel.application.services.InAppNotificationService;
import com.hotel.application.services.SseNotificationService;
import com.hotel.web.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final SseNotificationService sseNotificationService;
    private final InAppNotificationService inAppNotificationService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "guest";
        String role = "ROLE_CUSTOMER";
        if (authentication != null && !authentication.getAuthorities().isEmpty()) {
            role = authentication.getAuthorities().iterator().next().getAuthority();
        }
        return sseNotificationService.createEmitter(username, role);
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @RequestParam(defaultValue = "15") int limit) {
        return ResponseEntity.ok(inAppNotificationService.getMyNotifications(limit));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        long count = inAppNotificationService.getUnreadCount();
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(inAppNotificationService.markAsRead(id));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        inAppNotificationService.markAllAsRead();
        return ResponseEntity.noContent().build();
    }
}
