package com.hotel.web.dto;

import java.time.LocalDateTime;

public record RoomImageResponse(
        Long id,
        Long roomTypeId,
        String imageUrl,
        Boolean isCover,
        Integer displayOrder,
        LocalDateTime createdAt
) {}
