package com.hotel.web.dto;

import java.time.LocalDateTime;

public record ConciergeMessageResponse(
        Long id,
        Long senderId,
        String senderName,
        String senderRole,
        Long recipientId,
        Long reservationId,
        String suiteNumber,
        String message,
        boolean readStatus,
        LocalDateTime createdAt
) {
}
