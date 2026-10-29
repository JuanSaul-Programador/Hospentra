package com.hotel.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuitePerformanceDto {

    private Long roomId;
    private String roomNumber;
    private String roomTypeName;
    private long nightsOccupied;
    private double revenue;
    private double occupancyRate;
    private double adr;
}
