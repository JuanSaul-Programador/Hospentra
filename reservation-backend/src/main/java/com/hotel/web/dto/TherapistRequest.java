package com.hotel.web.dto;

import jakarta.validation.constraints.NotBlank;

public record TherapistRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String name,
        @NotBlank(message = "La especialidad es obligatoria")
        String specialty,
        String phone,
        Boolean active
) {}
