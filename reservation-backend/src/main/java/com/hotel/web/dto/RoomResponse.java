package com.hotel.web.dto;

import com.hotel.domain.model.RoomStatus;

public record RoomResponse(
        Long id,
        String roomNumber,
        RoomStatus status,
        RoomTypeResponse roomType
) {}
