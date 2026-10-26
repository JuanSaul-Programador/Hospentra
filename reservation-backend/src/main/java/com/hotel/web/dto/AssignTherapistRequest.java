package com.hotel.web.dto;

import jakarta.validation.constraints.NotNull;

public record AssignTherapistRequest(
        @NotNull(message = "El id del terapeuta es obligatorio")
        Long therapistId
) {}
