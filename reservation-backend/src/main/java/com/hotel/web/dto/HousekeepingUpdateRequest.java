package com.hotel.web.dto;

import com.hotel.domain.model.RoomStatus;

public record HousekeepingUpdateRequest(
        String housekeeperName,
        String housekeepingNotes,
        String cleaningPriority,
        RoomStatus status,
        Boolean markReady
) {}
