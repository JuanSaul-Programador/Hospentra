package com.hotel.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RequestAccountDeletionRequest(
        @NotBlank String password,
        String reason
) {}
