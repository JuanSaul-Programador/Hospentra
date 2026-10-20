package com.hotel.web.dto;

import com.hotel.domain.model.RoomStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoomRequest(
        @NotBlank String roomNumber,
        @NotNull RoomStatus status,
        @NotNull Long roomTypeId
) {}
