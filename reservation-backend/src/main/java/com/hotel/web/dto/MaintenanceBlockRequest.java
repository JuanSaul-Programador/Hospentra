package com.hotel.web.dto;

import com.hotel.domain.model.BlockType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

public record MaintenanceBlockRequest(
        @NotNull(message = "El identificador de habitación es obligatorio")
        Long roomId,

        @NotNull(message = "El tipo de bloqueo es obligatorio")
        BlockType blockType,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate startDate,

        @NotNull(message = "La fecha de finalización es obligatoria")
        LocalDate endDate,

        @NotBlank(message = "El motivo de mantenimiento es obligatorio")
        String reason,

        String contractorName
) {}
