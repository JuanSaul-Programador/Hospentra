package com.hotel.web.dto;

import com.hotel.domain.model.BlockStatus;
import com.hotel.domain.model.BlockType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceBlockResponse {
    private Long id;
    private Long roomId;
    private String roomNumber;
    private String roomTypeName;
    private BlockType blockType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private String contractorName;
    private BlockStatus status;
    private String createdBy;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private long durationDays;
}
