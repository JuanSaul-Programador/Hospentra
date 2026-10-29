package com.hotel.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelAnalyticsResponse {

    private double totalRevenue;
    private double roomRevenue;
    private double extrasRevenue;
    private double gastronomyRevenue;
    private double barRevenue;
    private double spaRevenue;
    private long totalReservations;
    private long totalNightsSold;
    private double occupancyRate;
    private double adr;
    private double revpar;
    private List<SuitePerformanceDto> suitePerformances;
    private List<MonthlyTrendDto> monthlyTrends;
}
