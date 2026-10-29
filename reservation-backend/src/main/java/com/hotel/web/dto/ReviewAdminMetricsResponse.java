package com.hotel.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewAdminMetricsResponse {

    private long totalReviews;
    private double averageRating;
    private long unansweredCount;
    private long answeredCount;
    private double satisfactionRate;
}
