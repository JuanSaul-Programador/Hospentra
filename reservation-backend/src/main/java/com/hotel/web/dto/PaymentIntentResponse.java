package com.hotel.web.dto;

import java.math.BigDecimal;

public record PaymentIntentResponse(
        String clientSecret,
        String paymentIntentId,
        BigDecimal amount,
        String currency,
        String status
) {}
