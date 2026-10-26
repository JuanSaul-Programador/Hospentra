package com.hotel.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateRoomOrderRequest(
        @NotNull Long reservationId,
        String notes,
        @NotEmpty @Valid List<CreateOrderItemRequest> items
) {}
