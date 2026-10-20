package com.hotel.web.dto;

import java.math.BigDecimal;

public record RoomTypeResponse(
        Long id,
        String name,
        String description,
        int capacity,
        BigDecimal pricePerNight
) {}
