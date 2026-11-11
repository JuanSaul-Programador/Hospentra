package com.hotel.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CheckOutSettlementRequest(
        @Size(max = 50) String settlementMethod,
        @DecimalMin("0.00") BigDecimal settlementAmount,
        @Size(max = 100) String authCode,
        @Size(max = 500) String notes
) {}
