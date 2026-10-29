package com.hotel.web.controller;

import com.hotel.application.services.ReviewService;
import com.hotel.web.dto.ManagementResponseRequest;
import com.hotel.web.dto.ReviewAdminMetricsResponse;
import com.hotel.web.dto.ReviewRequest;
import com.hotel.web.dto.ReviewResponse;
import com.hotel.web.dto.ReviewSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody ReviewRequest request, Principal principal) {
        ReviewResponse response = reviewService.createReview(request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByRoom(
            @PathVariable Long roomId,
            @RequestParam(required = false) Integer rating,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<ReviewResponse> page = reviewService.getReviewsByRoom(roomId, rating, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/room/{roomId}/summary")
    public ResponseEntity<ReviewSummaryResponse> getReviewSummary(@PathVariable Long roomId) {
        ReviewSummaryResponse summary = reviewService.getReviewSummary(roomId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<ReviewResponse> getReviewByReservation(@PathVariable Long reservationId) {
        ReviewResponse response = reviewService.getReviewByReservation(reservationId);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/management-response")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReviewResponse> addManagementResponse(
            @PathVariable Long id,
            @Valid @RequestBody ManagementResponseRequest request) {
        ReviewResponse response = reviewService.addManagementResponse(id, request.getResponse());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<Page<ReviewResponse>> getReviewsForAdmin(
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Boolean unansweredOnly,
            @PageableDefault(size = 15) Pageable pageable) {
        Page<ReviewResponse> page = reviewService.getReviewsForAdmin(rating, unansweredOnly, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/admin/metrics")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<ReviewAdminMetricsResponse> getAdminMetrics() {
        ReviewAdminMetricsResponse metrics = reviewService.getAdminMetrics();
        return ResponseEntity.ok(metrics);
    }
}
