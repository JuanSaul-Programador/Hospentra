package com.hotel.web.dto;

import com.hotel.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RoomOrderResponse(
        Long id,
        Long reservationId,
        String roomNumber,
        String guestName,
        BigDecimal totalAmount,
        OrderStatus status,
        String notes,
        LocalDateTime orderedAt,
        LocalDateTime deliveredAt,
        List<RoomOrderItemResponse> items
) {}
