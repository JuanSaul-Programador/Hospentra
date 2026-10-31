package com.hotel.web.dto;

import com.hotel.domain.model.Role;
import com.hotel.domain.model.Shift;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStaffRequest(
        @NotBlank @Email String email,
        @NotNull Role role,
        @NotNull Shift shift,
        @Size(min = 6) String password
) {}
