package com.hotel.web.dto;

import com.hotel.domain.model.Role;
import com.hotel.domain.model.Shift;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateStaffRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "La contraseña debe contener al menos una mayúscula, una minúscula y un número"
        )
        String password,
        @NotNull Role role,
        Shift shift
) {}
