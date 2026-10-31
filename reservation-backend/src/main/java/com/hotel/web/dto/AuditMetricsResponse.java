package com.hotel.web.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditMetricsResponse {
    private long totalEvents;
    private long todayEvents;
    private long failedEvents;
    private long distinctOperators;
}
