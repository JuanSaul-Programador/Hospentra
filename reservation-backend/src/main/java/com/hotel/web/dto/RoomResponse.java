package com.hotel.web.dto;

import com.hotel.domain.model.RoomStatus;
import java.time.LocalDateTime;

public record RoomResponse(
        Long id,
        String roomNumber,
        RoomStatus status,
        RoomTypeResponse roomType,
        String housekeeperName,
        String housekeepingNotes,
        LocalDateTime lastCleanedAt,
        String cleaningPriority
) {
    public RoomResponse(Long id, String roomNumber, RoomStatus status, RoomTypeResponse roomType) {
        this(id, roomNumber, status, roomType, null, null, null, "NORMAL");
    }
}
