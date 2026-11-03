package com.hotel.web.dto;

import java.time.LocalDateTime;

public record ConciergeConversationSummary(
        Long guestUserId,
        String guestName,
        String guestEmail,
        String suiteNumber,
        String lastMessage,
        LocalDateTime lastMessageAt,
        long unreadCount,
        String claimedBy,
        Long reservationId
) {
}
