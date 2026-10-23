package com.hotel.web.dto;

import com.hotel.domain.model.PaymentMethod;
import com.hotel.domain.model.PaymentStatus;
import com.hotel.domain.model.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        GuestResponse guest,
        RoomResponse room,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        ReservationStatus status,
        BigDecimal totalPrice,
        LocalDateTime createdAt,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        String cardLast4
) {}
