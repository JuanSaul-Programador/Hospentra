package com.hotel.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentIntentRequest(
        @NotNull @DecimalMin("1.00") BigDecimal amount,
        String currency,
        String description,
        String guestEmail
) {}
