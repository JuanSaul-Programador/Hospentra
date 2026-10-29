package com.hotel.application.services;

import com.hotel.web.dto.ReviewAdminMetricsResponse;
import com.hotel.web.dto.ReviewRequest;
import com.hotel.web.dto.ReviewResponse;
import com.hotel.web.dto.ReviewSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    ReviewResponse createReview(ReviewRequest request, String currentUsername);

    Page<ReviewResponse> getReviewsByRoom(Long roomId, Integer ratingFilter, Pageable pageable);

    ReviewSummaryResponse getReviewSummary(Long roomId);

    ReviewResponse addManagementResponse(Long reviewId, String responseText);

    ReviewResponse getReviewByReservation(Long reservationId);

    boolean hasReviewed(Long reservationId);

    Page<ReviewResponse> getReviewsForAdmin(Integer ratingFilter, Boolean unansweredOnly, Pageable pageable);

    ReviewAdminMetricsResponse getAdminMetrics();
}
