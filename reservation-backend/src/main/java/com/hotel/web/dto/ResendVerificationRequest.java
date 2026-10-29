package com.hotel.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendVerificationRequest(
        @NotBlank(message = "El correo electrónico es requerido")
        @Email(message = "Formato de correo inválido")
        String email
) {}
