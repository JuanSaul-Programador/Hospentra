package com.hotel.web.dto;

import com.hotel.domain.model.ExtraCategory;

import java.math.BigDecimal;

public record RoomOrderItemResponse(
        Long id,
        Long extraServiceId,
        String serviceName,
        ExtraCategory category,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal
) {}
