package com.hotel.web.controller;

import com.hotel.application.services.AnalyticsService;
import com.hotel.web.dto.HotelAnalyticsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private AnalyticsController analyticsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(analyticsController).build();
    }

    @Test
    void getExecutiveAnalytics_Returns200() throws Exception {
        HotelAnalyticsResponse response = HotelAnalyticsResponse.builder()
                .totalRevenue(5000.0)
                .roomRevenue(4500.0)
                .extrasRevenue(500.0)
                .totalReservations(5)
                .totalNightsSold(10)
                .adr(450.0)
                .occupancyRate(65.0)
                .revpar(292.5)
                .suitePerformances(Collections.emptyList())
                .monthlyTrends(Collections.emptyList())
                .build();

        when(analyticsService.getExecutiveAnalytics()).thenReturn(response);

        mockMvc.perform(get("/api/admin/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(5000.0))
                .andExpect(jsonPath("$.adr").value(450.0))
                .andExpect(jsonPath("$.occupancyRate").value(65.0));
    }

    @Test
    void getExecutiveAnalytics_WhenNoData_ReturnsDefaultValues() throws Exception {
        HotelAnalyticsResponse response = HotelAnalyticsResponse.builder()
                .totalRevenue(0.0)
                .roomRevenue(0.0)
                .extrasRevenue(0.0)
                .adr(0.0)
                .revpar(0.0)
                .occupancyRate(0.0)
                .totalReservations(0)
                .totalNightsSold(0L)
                .suitePerformances(Collections.emptyList())
                .monthlyTrends(Collections.emptyList())
                .build();

        when(analyticsService.getExecutiveAnalytics()).thenReturn(response);

        mockMvc.perform(get("/api/admin/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRevenue").value(0.0))
                .andExpect(jsonPath("$.adr").value(0.0))
                .andExpect(jsonPath("$.revpar").value(0.0))
                .andExpect(jsonPath("$.totalReservations").value(0));
    }
}
