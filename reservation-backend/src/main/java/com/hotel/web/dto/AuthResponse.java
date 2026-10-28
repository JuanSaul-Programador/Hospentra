package com.hotel.web.dto;

import com.hotel.domain.model.Role;

public record AuthResponse(
        String token,
        String refreshToken,
        String type,
        String username,
        String email,
        Role role
) {
    public AuthResponse(String token, String type, String username, String email, Role role) {
        this(token, null, type, username, email, role);
    }
}
