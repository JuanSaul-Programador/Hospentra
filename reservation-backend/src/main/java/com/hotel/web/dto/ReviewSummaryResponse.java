package com.hotel.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummaryResponse {

    private Long roomId;
    private Double averageRating;
    private Long totalReviews;
    private Double cleanlinessAverage;
    private Double serviceAverage;
    private Double comfortAverage;
    private Double locationAverage;
    private Map<Integer, Long> ratingDistribution;
    private Map<Integer, Double> ratingPercentages;
}
