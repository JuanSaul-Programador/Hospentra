package com.hotel.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailRequest(
        @NotBlank(message = "El correo electrónico es requerido")
        @Email(message = "Formato de correo inválido")
        String email,

        @NotBlank(message = "El código de verificación es requerido")
        @Pattern(regexp = "^\\d{6}$", message = "El código debe contener exactamente 6 dígitos numéricos")
        String code
) {}
