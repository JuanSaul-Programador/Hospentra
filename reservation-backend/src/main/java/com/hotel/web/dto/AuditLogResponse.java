package com.hotel.web.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {
    private Long id;
    private String username;
    private String userRole;
    private String action;
    private String entityName;
    private String entityId;
    private String details;
    private String ipAddress;
    private String status;
    private LocalDateTime createdAt;
}
