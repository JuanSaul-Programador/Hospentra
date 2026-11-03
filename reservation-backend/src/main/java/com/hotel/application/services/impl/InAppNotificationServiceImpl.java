package com.hotel.application.services.impl;

import com.hotel.application.dto.notification.NotificationEvent;
import com.hotel.application.services.InAppNotificationService;
import com.hotel.application.services.SseNotificationService;
import com.hotel.domain.model.Notification;
import com.hotel.domain.model.User;
import com.hotel.domain.repository.NotificationRepository;
import com.hotel.domain.repository.UserRepository;
import com.hotel.web.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InAppNotificationServiceImpl implements InAppNotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SseNotificationService sseNotificationService;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(int limit) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return Collections.emptyList();
        }

        int pageSize = Math.max(1, Math.min(limit, 50));
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId(), PageRequest.of(0, pageSize))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return 0L;
        }
        return notificationRepository.countByUserIdAndReadStatusFalse(currentUser.getId());
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long id) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new AccessDeniedException("Usuario no autenticado");
        }

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada: " + id));

        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("No tiene permiso para modificar esta notificación");
        }

        notification.setReadStatus(true);
        Notification saved = notificationRepository.save(notification);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            notificationRepository.markAllAsReadByUserId(currentUser.getId());
        }
    }

    @Override
    @Transactional
    public NotificationResponse createNotification(User user, String title, String message, String type, String link) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .readStatus(false)
                .link(link)
                .build();

        Notification saved = notificationRepository.save(notification);
        NotificationResponse response = toResponse(saved);

        NotificationEvent event = NotificationEvent.of(
                type,
                "USER",
                saved.getId(),
                title,
                message,
                response
        );
        sseNotificationService.sendToUser(user.getUsername(), event);

        return response;
    }

    @Override
    @Transactional
    public NotificationResponse createNotificationForUsername(String username, String title, String message, String type, String link) {
        return userRepository.findByUsername(username)
                .map(user -> createNotification(user, title, message, type, link))
                .orElse(null);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getTitle(),
                n.getMessage(),
                n.getType(),
                n.isReadStatus(),
                n.getLink(),
                n.getCreatedAt()
        );
    }
}
