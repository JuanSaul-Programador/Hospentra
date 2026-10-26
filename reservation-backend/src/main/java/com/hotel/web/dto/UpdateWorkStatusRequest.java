package com.hotel.web.dto;

import com.hotel.domain.model.WorkStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateWorkStatusRequest(
        @NotNull WorkStatus workStatus
) {}
