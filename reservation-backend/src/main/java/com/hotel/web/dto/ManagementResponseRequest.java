package com.hotel.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagementResponseRequest {

    @NotBlank(message = "La respuesta de la administración es obligatoria")
    @Size(min = 5, max = 2000, message = "La respuesta debe tener entre 5 y 2000 caracteres")
    private String response;
}
