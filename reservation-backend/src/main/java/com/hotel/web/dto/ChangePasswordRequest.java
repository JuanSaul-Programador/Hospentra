package com.hotel.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String code,
        @NotBlank
        @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "La nueva contraseña debe contener al menos una mayúscula, una minúscula y un número"
        )
        String newPassword
) {}
