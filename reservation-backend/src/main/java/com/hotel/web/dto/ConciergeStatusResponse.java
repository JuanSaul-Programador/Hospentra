package com.hotel.web.dto;

public record ConciergeStatusResponse(
        boolean activeStay,
        String suiteNumber,
        Long reservationId,
        String guestName
) {
}
