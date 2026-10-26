package com.hotel.web.dto;

import com.hotel.domain.model.Role;
import com.hotel.domain.model.Shift;
import com.hotel.domain.model.WorkStatus;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        Role role,
        boolean enabled,
        LocalDateTime createdAt,
        Shift shift,
        WorkStatus workStatus,
        LocalDateTime breakStartedAt
) {}
