package com.hotel.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConciergeMessageRequest(
        @NotBlank(message = "El mensaje no puede estar vacío")
        @Size(max = 2000, message = "El mensaje no puede superar 2000 caracteres")
        String message,

        Long reservationId,

        String suiteNumber,

        Long recipientId
) {
}
