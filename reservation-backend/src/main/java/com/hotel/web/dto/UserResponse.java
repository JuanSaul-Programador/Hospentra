package com.hotel.web.dto;

import com.hotel.domain.model.Role;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        Role role,
        boolean enabled,
        LocalDateTime createdAt
) {}
