package com.hotel.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RoomTypeRequest(
        @NotBlank String name,
        String description,
        @Positive int capacity,
        @NotNull @Positive BigDecimal pricePerNight
) {}
