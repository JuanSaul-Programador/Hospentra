package com.hotel.web.dto;

import java.math.BigDecimal;

public record RoomOrderItemResponse(
        Long id,
        Long extraServiceId,
        String serviceName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal
) {}
