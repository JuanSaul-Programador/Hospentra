package com.hotel.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PreCheckInRequest(
        @NotBlank(message = "El tipo de documento es obligatorio")
        @Size(max = 30, message = "El tipo de documento no puede exceder 30 caracteres")
        String documentType,

        @NotBlank(message = "El número de documento es obligatorio")
        @Size(max = 50, message = "El número de documento no puede exceder 50 caracteres")
        String documentNumber,

        @NotBlank(message = "La nacionalidad es obligatoria")
        @Size(max = 80, message = "La nacionalidad no puede exceder 80 caracteres")
        String nationality,

        @NotBlank(message = "La hora estimada de llegada es obligatoria")
        @Size(max = 20, message = "La hora estimada no puede exceder 20 caracteres")
        String estimatedArrivalTime,

        @Size(max = 500, message = "Las solicitudes especiales no pueden exceder 500 caracteres")
        String specialRequests
) {}
