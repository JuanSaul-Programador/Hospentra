package com.hotel.web.dto;

import java.math.BigDecimal;

public record CheckOutSettlementRequest(
        String settlementMethod,
        BigDecimal settlementAmount,
        String authCode,
        String notes
) {}
