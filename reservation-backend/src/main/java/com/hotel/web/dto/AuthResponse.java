package com.hotel.web.dto;

import com.hotel.domain.model.Role;

public record AuthResponse(
        String token,
        String type,
        String username,
        String email,
        Role role
) {}
