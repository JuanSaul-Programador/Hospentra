package com.hotel.web.dto;

import com.hotel.domain.model.Role;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        Role role,
        boolean emailVerified,
        String firstName,
        String lastName,
        String phone,
        LocalDateTime deletionRequestedAt,
        LocalDateTime scheduledDeletionDate,
        boolean pendingDeletion
) {}
