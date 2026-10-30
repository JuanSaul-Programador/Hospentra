package com.hotel.web.dto;

import java.math.BigDecimal;

public record RoomTypeResponse(
        Long id,
        String name,
        String nameEn,
        String description,
        String descriptionEn,
        int capacity,
        BigDecimal pricePerNight,
        String coverImageUrl
) {}
