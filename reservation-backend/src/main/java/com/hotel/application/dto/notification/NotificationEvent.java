package com.hotel.application.dto.notification;

import java.time.LocalDateTime;

public record NotificationEvent(
        String eventType,
        String targetChannel,
        Long entityId,
        String title,
        String message,
        Object payload,
        LocalDateTime timestamp
) {
    public static NotificationEvent of(String eventType, String targetChannel, Long entityId, String title, String message, Object payload) {
        return new NotificationEvent(eventType, targetChannel, entityId, title, message, payload, LocalDateTime.now());
    }
}
