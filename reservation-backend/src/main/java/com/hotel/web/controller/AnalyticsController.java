package com.hotel.web.controller;

import com.hotel.application.services.AnalyticsService;
import com.hotel.web.dto.HotelAnalyticsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<HotelAnalyticsResponse> getExecutiveAnalytics() {
        HotelAnalyticsResponse response = analyticsService.getExecutiveAnalytics();
        return ResponseEntity.ok(response);
    }
}
