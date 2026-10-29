package com.hotel.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyTrendDto {

    private String month;
    private double roomRevenue;
    private double extrasRevenue;
    private double totalRevenue;
    private double occupancyRate;
}
