package com.hotel.application.services.impl;

import com.hotel.application.services.ReviewService;
import com.hotel.domain.model.Reservation;
import com.hotel.domain.model.ReservationStatus;
import com.hotel.domain.model.Review;
import com.hotel.domain.model.Role;
import com.hotel.domain.model.User;
import com.hotel.domain.repository.ReservationRepository;
import com.hotel.domain.repository.ReviewRepository;
import com.hotel.domain.repository.UserRepository;
import com.hotel.web.dto.ReviewAdminMetricsResponse;
import com.hotel.web.dto.ReviewRequest;
import com.hotel.web.dto.ReviewResponse;
import com.hotel.web.dto.ReviewSummaryResponse;
import com.hotel.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;

    @Override
    public ReviewResponse createReview(ReviewRequest request, String currentUsername) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + currentUsername));

        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new ResourceNotFoundException("Reservación no encontrada con id: " + request.getReservationId()));

        if (user.getRole() != Role.ROLE_ADMIN) {
            if (user.getGuest() == null || !reservation.getGuest().getId().equals(user.getGuest().getId())) {
                throw new IllegalArgumentException("No tiene autorización para calificar una reservación ajena");
            }
        }

        if (reservation.getStatus() != ReservationStatus.COMPLETED) {
            throw new IllegalStateException("Solo puede calificar una estancia que haya concluido");
        }

        if (reviewRepository.existsByReservationId(reservation.getId())) {
            throw new IllegalStateException("Esta reservación ya cuenta con una reseña registrada");
        }

        Review review = Review.builder()
                .reservation(reservation)
                .user(user)
                .room(reservation.getRoom())
                .rating(request.getRating())
                .cleanlinessRating(request.getCleanlinessRating() != null ? request.getCleanlinessRating() : request.getRating())
                .serviceRating(request.getServiceRating() != null ? request.getServiceRating() : request.getRating())
                .comfortRating(request.getComfortRating() != null ? request.getComfortRating() : request.getRating())
                .locationRating(request.getLocationRating() != null ? request.getLocationRating() : request.getRating())
                .title(request.getTitle())
                .comment(request.getComment())
                .build();

        Review saved = reviewRepository.save(review);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByRoom(Long roomId, Integer ratingFilter, Pageable pageable) {
        if (ratingFilter != null && ratingFilter >= 1 && ratingFilter <= 5) {
            return reviewRepository.findByRoomIdAndRatingOrderByCreatedAtDesc(roomId, ratingFilter, pageable)
                    .map(this::toResponse);
        }
        return reviewRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewSummaryResponse getReviewSummary(Long roomId) {
        Double avg = reviewRepository.calculateAverageRatingByRoomId(roomId);
        double roundedAvg = avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;

        Long total = reviewRepository.countByRoomId(roomId);
        long totalCount = total != null ? total : 0L;

        Map<Integer, Long> distribution = new LinkedHashMap<>();
        distribution.put(5, 0L);
        distribution.put(4, 0L);
        distribution.put(3, 0L);
        distribution.put(2, 0L);
        distribution.put(1, 0L);

        List<Object[]> counts = reviewRepository.countRatingsByRoomId(roomId);
        for (Object[] row : counts) {
            Integer r = (Integer) row[0];
            Long c = (Long) row[1];
            if (r != null && distribution.containsKey(r)) {
                distribution.put(r, c);
            }
        }

        Map<Integer, Double> percentages = new LinkedHashMap<>();
        for (Map.Entry<Integer, Long> entry : distribution.entrySet()) {
            double pct = totalCount > 0 ? Math.round((entry.getValue() * 100.0 / totalCount) * 10.0) / 10.0 : 0.0;
            percentages.put(entry.getKey(), pct);
        }

        List<Object[]> dims = reviewRepository.calculateDimensionalAverages(roomId);
        Double cleanAvg = null;
        Double servAvg = null;
        Double comfAvg = null;
        Double locAvg = null;

        if (dims != null && !dims.isEmpty() && dims.get(0) != null) {
            Object[] row = dims.get(0);
            if (row[0] != null) cleanAvg = Math.round(((Double) row[0]) * 10.0) / 10.0;
            if (row[1] != null) servAvg = Math.round(((Double) row[1]) * 10.0) / 10.0;
            if (row[2] != null) comfAvg = Math.round(((Double) row[2]) * 10.0) / 10.0;
            if (row[3] != null) locAvg = Math.round(((Double) row[3]) * 10.0) / 10.0;
        }

        return ReviewSummaryResponse.builder()
                .roomId(roomId)
                .averageRating(roundedAvg)
                .totalReviews(totalCount)
                .cleanlinessAverage(cleanAvg != null ? cleanAvg : roundedAvg)
                .serviceAverage(servAvg != null ? servAvg : roundedAvg)
                .comfortAverage(comfAvg != null ? comfAvg : roundedAvg)
                .locationAverage(locAvg != null ? locAvg : roundedAvg)
                .ratingDistribution(distribution)
                .ratingPercentages(percentages)
                .build();
    }

    @Override
    public ReviewResponse addManagementResponse(Long reviewId, String responseText) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Reseña no encontrada con id: " + reviewId));

        review.setManagementResponse(responseText);
        review.setManagementRespondedAt(LocalDateTime.now());
        Review updated = reviewRepository.save(review);
        return toResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReviewByReservation(Long reservationId) {
        return reviewRepository.findByReservationId(reservationId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasReviewed(Long reservationId) {
        return reviewRepository.existsByReservationId(reservationId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsForAdmin(Integer ratingFilter, Boolean unansweredOnly, Pageable pageable) {
        return reviewRepository.findAdminReviews(ratingFilter, unansweredOnly, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewAdminMetricsResponse getAdminMetrics() {
        long total = reviewRepository.count();
        long unanswered = reviewRepository.countByManagementResponseIsNull();
        long answered = total - unanswered;
        Double avg = reviewRepository.calculateOverallAverageRating();
        double roundedAvg = avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;

        double satisfactionRate = 0.0;
        if (total > 0) {
            long positive = reviewRepository.findAll().stream()
                    .filter(r -> r.getRating() != null && r.getRating() >= 4)
                    .count();
            satisfactionRate = Math.round((positive * 100.0 / total) * 10.0) / 10.0;
        }

        return ReviewAdminMetricsResponse.builder()
                .totalReviews(total)
                .averageRating(roundedAvg)
                .unansweredCount(unanswered)
                .answeredCount(answered)
                .satisfactionRate(satisfactionRate)
                .build();
    }

    private ReviewResponse toResponse(Review review) {
        String guestName = "Huésped Distinguido";
        if (review.getReservation() != null && review.getReservation().getGuest() != null) {
            guestName = review.getReservation().getGuest().getFirstName() + " " + review.getReservation().getGuest().getLastName();
        } else if (review.getUser() != null) {
            guestName = review.getUser().getUsername();
        }

        String roomTypeName = "";
        String roomTypeNameEn = "";
        if (review.getRoom() != null && review.getRoom().getRoomType() != null) {
            roomTypeName = review.getRoom().getRoomType().getName();
            roomTypeNameEn = review.getRoom().getRoomType().getNameEn();
        }

        return ReviewResponse.builder()
                .id(review.getId())
                .reservationId(review.getReservation() != null ? review.getReservation().getId() : null)
                .roomId(review.getRoom() != null ? review.getRoom().getId() : null)
                .roomNumber(review.getRoom() != null ? review.getRoom().getRoomNumber() : null)
                .roomTypeName(roomTypeName)
                .roomTypeNameEn(roomTypeNameEn)
                .userId(review.getUser() != null ? review.getUser().getId() : null)
                .guestName(guestName)
                .rating(review.getRating())
                .cleanlinessRating(review.getCleanlinessRating())
                .serviceRating(review.getServiceRating())
                .comfortRating(review.getComfortRating())
                .locationRating(review.getLocationRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .managementResponse(review.getManagementResponse())
                .managementRespondedAt(review.getManagementRespondedAt())
                .createdAt(review.getCreatedAt())
                .verifiedStay(true)
                .build();
    }
}
