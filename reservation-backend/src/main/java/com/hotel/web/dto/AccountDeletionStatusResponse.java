package com.hotel.web.dto;

import java.time.LocalDateTime;

public record AccountDeletionStatusResponse(
        boolean pendingDeletion,
        LocalDateTime deletionRequestedAt,
        LocalDateTime scheduledDeletionDate,
        String message
) {}
