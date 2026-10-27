package com.hotel.application.services;

import com.hotel.application.dto.notification.NotificationEvent;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseNotificationService {
    SseEmitter createEmitter(String username, String role);
    void broadcast(NotificationEvent event);
    void sendToRole(String role, NotificationEvent event);
    void sendToUser(String username, NotificationEvent event);
}
