package com.hotel.web.dto;

import com.hotel.domain.model.PaymentMethod;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ReservationRequest(
        Long guestId,
        @NotNull Long roomId,
        @NotNull @FutureOrPresent LocalDate checkInDate,
        @NotNull LocalDate checkOutDate,
        PaymentMethod paymentMethod,
        String cardLast4,
        String paymentIntentId,
        String authCode
) {
    public ReservationRequest(Long guestId, Long roomId, LocalDate checkInDate, LocalDate checkOutDate, PaymentMethod paymentMethod, String cardLast4) {
        this(guestId, roomId, checkInDate, checkOutDate, paymentMethod, cardLast4, null, null);
    }
}
