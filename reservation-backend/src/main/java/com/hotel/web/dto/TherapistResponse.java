package com.hotel.web.dto;

import java.time.LocalDateTime;

public record TherapistResponse(
        Long id,
        String name,
        String specialty,
        String phone,
        Boolean active,
        LocalDateTime createdAt
) {}
