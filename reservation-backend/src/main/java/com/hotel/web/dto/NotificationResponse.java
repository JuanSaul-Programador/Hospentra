package com.hotel.web.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String title,
        String message,
        String type,
        boolean read,
        String link,
        LocalDateTime createdAt
) {}
