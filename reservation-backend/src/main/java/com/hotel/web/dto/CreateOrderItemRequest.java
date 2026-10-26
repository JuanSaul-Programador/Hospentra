package com.hotel.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateOrderItemRequest(
        @NotNull Long extraServiceId,
        @NotNull @Min(1) Integer quantity
) {}
