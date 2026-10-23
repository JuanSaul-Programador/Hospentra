package com.hotel.web.dto;

import com.hotel.domain.model.PaymentMethod;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ReservationRequest(
        Long guestId,
        @NotNull Long roomId,
        @NotNull @Future LocalDate checkInDate,
        @NotNull LocalDate checkOutDate,
        PaymentMethod paymentMethod,
        String cardLast4
) {}
