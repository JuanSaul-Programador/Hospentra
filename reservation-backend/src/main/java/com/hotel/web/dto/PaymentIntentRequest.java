package com.hotel.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentIntentRequest(
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        String currency,
        String description,
        String guestEmail
) {}
