package com.hotel.web.dto;

import com.hotel.domain.model.RoomStatus;
import jakarta.validation.constraints.Size;

public record HousekeepingUpdateRequest(
        @Size(max = 100) String housekeeperName,
        @Size(max = 1000) String housekeepingNotes,
        @Size(max = 20) String cleaningPriority,
        RoomStatus status,
        Boolean markReady
) {}
