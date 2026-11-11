package com.hotel.web.dto;

import com.hotel.domain.model.Role;
import com.hotel.domain.model.Shift;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateStaffRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotNull Role role,
        @NotNull Shift shift,
        @Pattern(
                regexp = "^$|^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "La contraseña debe tener al menos 8 caracteres, incluyendo mayúscula, minúscula y número"
        )
        String password
) {}
