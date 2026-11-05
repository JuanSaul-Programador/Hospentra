package com.hotel.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ExpressCheckOutRequest(
        @NotNull
        @DecimalMin("0.00")
        BigDecimal amount,
        @NotBlank
        String cardLast4,
        String paymentIntentId,
        String notes
) {}
