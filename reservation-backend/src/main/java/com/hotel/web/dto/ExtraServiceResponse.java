package com.hotel.web.dto;

import com.hotel.domain.model.ExtraCategory;

import java.math.BigDecimal;

public record ExtraServiceResponse(
        Long id,
        String name,
        String nameEn,
        ExtraCategory category,
        String description,
        String descriptionEn,
        BigDecimal price,
        String imageUrl,
        boolean available
) {}
