package com.hotel.application.services;

import com.hotel.application.services.impl.ReviewServiceImpl;
import com.hotel.domain.exception.ResourceNotFoundException;
import com.hotel.domain.model.Guest;
import com.hotel.domain.model.Reservation;
import com.hotel.domain.model.ReservationStatus;
import com.hotel.domain.model.Review;
import com.hotel.domain.model.Role;
import com.hotel.domain.model.Room;
import com.hotel.domain.model.RoomType;
import com.hotel.domain.model.User;
import com.hotel.domain.repository.ReservationRepository;
import com.hotel.domain.repository.ReviewRepository;
import com.hotel.domain.repository.UserRepository;
import com.hotel.web.dto.ReviewAdminMetricsResponse;
import com.hotel.web.dto.ReviewRequest;
import com.hotel.web.dto.ReviewResponse;
import com.hotel.web.dto.ReviewSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User testUser;
    private Guest testGuest;
    private Room testRoom;
    private RoomType testRoomType;
    private Reservation testReservation;

    @BeforeEach
    void setUp() {
        testGuest = Guest.builder()
                .id(10L)
                .firstName("Pepe")
                .lastName("Garcia")
                .email("pepe@gmail.com")
                .build();

        testUser = User.builder()
                .id(1L)
                .username("pepe")
                .email("pepe@gmail.com")
                .role(Role.ROLE_CUSTOMER)
                .guest(testGuest)
                .build();

        testRoomType = RoomType.builder()
                .id(1L)
                .name("Suite Royal")
                .nameEn("Royal Suite")
                .pricePerNight(BigDecimal.valueOf(380.00))
                .build();

        testRoom = Room.builder()
                .id(1L)
                .roomNumber("101")
                .roomType(testRoomType)
                .build();

        testReservation = Reservation.builder()
                .id(50L)
                .guest(testGuest)
                .room(testRoom)
                .checkInDate(LocalDate.now().minusDays(5))
                .checkOutDate(LocalDate.now().minusDays(2))
                .status(ReservationStatus.COMPLETED)
                .build();
    }

    @Test
    void createReview_Success() {
        ReviewRequest request = ReviewRequest.builder()
                .reservationId(50L)
                .rating(5)
                .cleanlinessRating(5)
                .serviceRating(4)
                .comfortRating(5)
                .locationRating(5)
                .title("Excelente estancia")
                .comment("Todo fue maravilloso y el servicio impecable.")
                .build();

        Review savedReview = Review.builder()
                .id(100L)
                .reservation(testReservation)
                .user(testUser)
                .room(testRoom)
                .rating(5)
                .cleanlinessRating(5)
                .serviceRating(4)
                .comfortRating(5)
                .locationRating(5)
                .title("Excelente estancia")
                .comment("Todo fue maravilloso y el servicio impecable.")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByUsername("pepe")).thenReturn(Optional.of(testUser));
        when(reservationRepository.findById(50L)).thenReturn(Optional.of(testReservation));
        when(reviewRepository.existsByReservationId(50L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        ReviewResponse response = reviewService.createReview(request, "pepe");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.isVerifiedStay()).isTrue();
        assertThat(response.getGuestName()).isEqualTo("Pepe Garcia");
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void createReview_WhenStatusPending_ThrowsIllegalStateException() {
        testReservation.setStatus(ReservationStatus.PENDING);

        ReviewRequest request = ReviewRequest.builder()
                .reservationId(50L)
                .rating(5)
                .comment("Intento de calificar reserva pendiente")
                .build();

        when(userRepository.findByUsername("pepe")).thenReturn(Optional.of(testUser));
        when(reservationRepository.findById(50L)).thenReturn(Optional.of(testReservation));

        assertThatThrownBy(() -> reviewService.createReview(request, "pepe"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo puede calificar una estancia que haya concluido");
    }

    @Test
    void createReview_WhenStatusConfirmed_ThrowsIllegalStateException() {
        testReservation.setStatus(ReservationStatus.CONFIRMED);

        ReviewRequest request = ReviewRequest.builder()
                .reservationId(50L)
                .rating(5)
                .comment("Intento de calificar reserva confirmada pero aun no completada")
                .build();

        when(userRepository.findByUsername("pepe")).thenReturn(Optional.of(testUser));
        when(reservationRepository.findById(50L)).thenReturn(Optional.of(testReservation));

        assertThatThrownBy(() -> reviewService.createReview(request, "pepe"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo puede calificar una estancia que haya concluido");
    }

    @Test
    void createReview_WhenStatusCheckedIn_ThrowsIllegalStateException() {
        testReservation.setStatus(ReservationStatus.CHECKED_IN);

        ReviewRequest request = ReviewRequest.builder()
                .reservationId(50L)
                .rating(4)
                .comment("Huésped aún en casa intentando calificar antes de check-out")
                .build();

        when(userRepository.findByUsername("pepe")).thenReturn(Optional.of(testUser));
        when(reservationRepository.findById(50L)).thenReturn(Optional.of(testReservation));

        assertThatThrownBy(() -> reviewService.createReview(request, "pepe"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo puede calificar una estancia que haya concluido");
    }

    @Test
    void createReview_WhenStatusCancelled_ThrowsIllegalStateException() {
        testReservation.setStatus(ReservationStatus.CANCELLED);

        ReviewRequest request = ReviewRequest.builder()
                .reservationId(50L)
                .rating(1)
                .comment("Intento de calificar una reserva que fue cancelada sin estadia")
                .build();

        when(userRepository.findByUsername("pepe")).thenReturn(Optional.of(testUser));
        when(reservationRepository.findById(50L)).thenReturn(Optional.of(testReservation));

        assertThatThrownBy(() -> reviewService.createReview(request, "pepe"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo puede calificar una estancia que haya concluido");
    }

    @Test
    void createReview_DifferentGuest_ThrowsIllegalArgumentException() {
        Guest anotherGuest = Guest.builder().id(99L).build();
        testReservation.setGuest(anotherGuest);

        ReviewRequest request = ReviewRequest.builder()
                .reservationId(50L)
                .rating(5)
                .comment("Intento de calificar reserva ajena")
                .build();

        when(userRepository.findByUsername("pepe")).thenReturn(Optional.of(testUser));
        when(reservationRepository.findById(50L)).thenReturn(Optional.of(testReservation));

        assertThatThrownBy(() -> reviewService.createReview(request, "pepe"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No tiene autorización para calificar una reservación ajena");
    }

    @Test
    void createReview_UserWithoutGuest_ThrowsIllegalArgumentException() {
        testUser.setGuest(null);

        ReviewRequest request = ReviewRequest.builder()
                .reservationId(50L)
                .rating(5)
                .comment("Usuario sin perfil de huésped")
                .build();

        when(userRepository.findByUsername("pepe")).thenReturn(Optional.of(testUser));
        when(reservationRepository.findById(50L)).thenReturn(Optional.of(testReservation));

        assertThatThrownBy(() -> reviewService.createReview(request, "pepe"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No tiene autorización para calificar una reservación ajena");
    }

    @Test
    void createReview_AdminRole_AllowedWithoutGuestProfile() {
        testUser.setRole(Role.ROLE_ADMIN);
        testUser.setGuest(null);

        ReviewRequest request = ReviewRequest.builder()
                .reservationId(50L)
                .rating(5)
                .comment("Reseña ingresada por administración")
                .build();

        Review savedReview = Review.builder()
                .id(101L)
                .reservation(testReservation)
                .user(testUser)
                .room(testRoom)
                .rating(5)
                .comment("Reseña ingresada por administración")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findByUsername("pepe")).thenReturn(Optional.of(testUser));
        when(reservationRepository.findById(50L)).thenReturn(Optional.of(testReservation));
        when(reviewRepository.existsByReservationId(50L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        ReviewResponse response = reviewService.createReview(request, "pepe");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(101L);
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void createReview_AlreadyReviewed_ThrowsIllegalStateException() {
        ReviewRequest request = ReviewRequest.builder()
                .reservationId(50L)
                .rating(5)
                .comment("Segunda resena")
                .build();

        when(userRepository.findByUsername("pepe")).thenReturn(Optional.of(testUser));
        when(reservationRepository.findById(50L)).thenReturn(Optional.of(testReservation));
        when(reviewRepository.existsByReservationId(50L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(request, "pepe"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Esta reservación ya cuenta con una reseña registrada");
    }

    @Test
    void createReview_ReservationNotFound_ThrowsResourceNotFoundException() {
        ReviewRequest request = ReviewRequest.builder()
                .reservationId(999L)
                .rating(5)
                .comment("Reserva inexistente")
                .build();

        when(userRepository.findByUsername("pepe")).thenReturn(Optional.of(testUser));
        when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(request, "pepe"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Reservación no encontrada");
    }

    @Test
    void createReview_UserNotFound_ThrowsResourceNotFoundException() {
        ReviewRequest request = ReviewRequest.builder()
                .reservationId(50L)
                .rating(5)
                .comment("Usuario inexistente")
                .build();

        when(userRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.createReview(request, "fantasma"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuario no encontrado");
    }

    @Test
    void getReviewSummary_CalculatesDistributionAndPercentages() {
        when(reviewRepository.calculateAverageRatingByRoomId(1L)).thenReturn(4.6);
        when(reviewRepository.countByRoomId(1L)).thenReturn(10L);

        List<Object[]> distributionList = new ArrayList<>();
        distributionList.add(new Object[]{5, 7L});
        distributionList.add(new Object[]{4, 2L});
        distributionList.add(new Object[]{3, 1L});
        when(reviewRepository.countRatingsByRoomId(1L)).thenReturn(distributionList);

        List<Object[]> dims = new ArrayList<>();
        dims.add(new Object[]{4.8, 4.5, 4.7, 4.9});
        when(reviewRepository.calculateDimensionalAverages(1L)).thenReturn(dims);

        ReviewSummaryResponse summary = reviewService.getReviewSummary(1L);

        assertThat(summary).isNotNull();
        assertThat(summary.getAverageRating()).isEqualTo(4.6);
        assertThat(summary.getTotalReviews()).isEqualTo(10L);
        assertThat(summary.getRatingDistribution().get(5)).isEqualTo(7L);
        assertThat(summary.getRatingDistribution().get(4)).isEqualTo(2L);
        assertThat(summary.getRatingDistribution().get(3)).isEqualTo(1L);
        assertThat(summary.getRatingDistribution().get(2)).isEqualTo(0L);
        assertThat(summary.getRatingDistribution().get(1)).isEqualTo(0L);
        assertThat(summary.getRatingPercentages().get(5)).isEqualTo(70.0);
        assertThat(summary.getRatingPercentages().get(4)).isEqualTo(20.0);
        assertThat(summary.getRatingPercentages().get(3)).isEqualTo(10.0);
        assertThat(summary.getCleanlinessAverage()).isEqualTo(4.8);
        assertThat(summary.getServiceAverage()).isEqualTo(4.5);
        assertThat(summary.getComfortAverage()).isEqualTo(4.7);
        assertThat(summary.getLocationAverage()).isEqualTo(4.9);
    }

    @Test
    void getReviewSummary_WhenNoReviews_ReturnsZeros() {
        when(reviewRepository.calculateAverageRatingByRoomId(2L)).thenReturn(null);
        when(reviewRepository.countByRoomId(2L)).thenReturn(0L);
        when(reviewRepository.countRatingsByRoomId(2L)).thenReturn(Collections.emptyList());
        when(reviewRepository.calculateDimensionalAverages(2L)).thenReturn(Collections.emptyList());

        ReviewSummaryResponse summary = reviewService.getReviewSummary(2L);

        assertThat(summary).isNotNull();
        assertThat(summary.getAverageRating()).isEqualTo(0.0);
        assertThat(summary.getTotalReviews()).isEqualTo(0L);
        assertThat(summary.getRatingDistribution().get(5)).isEqualTo(0L);
        assertThat(summary.getRatingPercentages().get(5)).isEqualTo(0.0);
    }

    @Test
    void getReviewsByRoom_WithRatingFilter_ReturnsFilteredPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Review rev = Review.builder()
                .id(1L)
                .rating(5)
                .comment("Cinco estrellas")
                .room(testRoom)
                .reservation(testReservation)
                .user(testUser)
                .build();
        Page<Review> page = new PageImpl<>(List.of(rev));

        when(reviewRepository.findByRoomIdAndRatingOrderByCreatedAtDesc(1L, 5, pageable)).thenReturn(page);

        Page<ReviewResponse> result = reviewService.getReviewsByRoom(1L, 5, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getRating()).isEqualTo(5);
    }

    @Test
    void getReviewsByRoom_WithoutRatingFilter_ReturnsAllPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Review rev1 = Review.builder().id(1L).rating(5).comment("Excelente").room(testRoom).reservation(testReservation).user(testUser).build();
        Review rev2 = Review.builder().id(2L).rating(4).comment("Muy bueno").room(testRoom).reservation(testReservation).user(testUser).build();
        Page<Review> page = new PageImpl<>(List.of(rev1, rev2));

        when(reviewRepository.findByRoomIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(page);

        Page<ReviewResponse> result = reviewService.getReviewsByRoom(1L, null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void getReviewByReservation_WhenFound_ReturnsResponse() {
        Review rev = Review.builder()
                .id(1L)
                .rating(5)
                .comment("Gran estancia")
                .room(testRoom)
                .reservation(testReservation)
                .user(testUser)
                .build();

        when(reviewRepository.findByReservationId(50L)).thenReturn(Optional.of(rev));

        ReviewResponse response = reviewService.getReviewByReservation(50L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void getReviewByReservation_WhenNotFound_ReturnsNull() {
        when(reviewRepository.findByReservationId(50L)).thenReturn(Optional.empty());

        ReviewResponse response = reviewService.getReviewByReservation(50L);

        assertThat(response).isNull();
    }

    @Test
    void hasReviewed_ReturnsTrueWhenExists() {
        when(reviewRepository.existsByReservationId(50L)).thenReturn(true);

        boolean exists = reviewService.hasReviewed(50L);

        assertThat(exists).isTrue();
    }

    @Test
    void hasReviewed_ReturnsFalseWhenNotExists() {
        when(reviewRepository.existsByReservationId(50L)).thenReturn(false);

        boolean exists = reviewService.hasReviewed(50L);

        assertThat(exists).isFalse();
    }

    @Test
    void addManagementResponse_Success() {
        Review existing = Review.builder()
                .id(20L)
                .rating(3)
                .comment("Hubo algo de ruido.")
                .room(testRoom)
                .reservation(testReservation)
                .build();

        when(reviewRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = reviewService.addManagementResponse(20L, "Lamentamos las molestias, lo hemos solucionado.");

        assertThat(response).isNotNull();
        assertThat(response.getManagementResponse()).isEqualTo("Lamentamos las molestias, lo hemos solucionado.");
        assertThat(response.getManagementRespondedAt()).isNotNull();
    }

    @Test
    void addManagementResponse_NotFound_ThrowsResourceNotFoundException() {
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.addManagementResponse(999L, "Respuesta"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Reseña no encontrada");
    }

    @Test
    void getReviewsForAdmin_ReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Review r1 = Review.builder().id(1L).rating(5).comment("Super").room(testRoom).reservation(testReservation).build();
        Page<Review> page = new PageImpl<>(List.of(r1), pageable, 1);

        when(reviewRepository.findAdminReviews(null, null, pageable)).thenReturn(page);

        Page<ReviewResponse> result = reviewService.getReviewsForAdmin(null, null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
    }

    @Test
    void getAdminMetrics_ReturnsCalculatedMetrics() {
        when(reviewRepository.count()).thenReturn(10L);
        when(reviewRepository.countByManagementResponseIsNull()).thenReturn(3L);
        when(reviewRepository.calculateOverallAverageRating()).thenReturn(4.6);

        Review r1 = Review.builder().id(1L).rating(5).build();
        Review r2 = Review.builder().id(2L).rating(4).build();
        Review r3 = Review.builder().id(3L).rating(3).build();
        when(reviewRepository.findAll()).thenReturn(List.of(r1, r2, r3));

        ReviewAdminMetricsResponse metrics = reviewService.getAdminMetrics();

        assertThat(metrics).isNotNull();
        assertThat(metrics.getTotalReviews()).isEqualTo(10L);
        assertThat(metrics.getUnansweredCount()).isEqualTo(3L);
        assertThat(metrics.getAnsweredCount()).isEqualTo(7L);
        assertThat(metrics.getAverageRating()).isEqualTo(4.6);
        assertThat(metrics.getSatisfactionRate()).isGreaterThan(0.0);
    }

    @Test
    void getAdminMetrics_WhenNoReviews_ReturnsZeroMetrics() {
        when(reviewRepository.count()).thenReturn(0L);
        when(reviewRepository.countByManagementResponseIsNull()).thenReturn(0L);
        when(reviewRepository.calculateOverallAverageRating()).thenReturn(null);

        ReviewAdminMetricsResponse metrics = reviewService.getAdminMetrics();

        assertThat(metrics).isNotNull();
        assertThat(metrics.getTotalReviews()).isEqualTo(0L);
        assertThat(metrics.getUnansweredCount()).isEqualTo(0L);
        assertThat(metrics.getAnsweredCount()).isEqualTo(0L);
        assertThat(metrics.getAverageRating()).isEqualTo(0.0);
        assertThat(metrics.getSatisfactionRate()).isEqualTo(0.0);
    }

    @Test
    void getReviewsForAdmin_WithFilters_AppliesFiltersCorrectly() {
        Pageable pageable = PageRequest.of(0, 10);
        Review r1 = Review.builder().id(2L).rating(5).room(testRoom).reservation(testReservation).build();
        Page<Review> page = new PageImpl<>(List.of(r1), pageable, 1);

        when(reviewRepository.findAdminReviews(5, true, pageable)).thenReturn(page);

        Page<ReviewResponse> result = reviewService.getReviewsForAdmin(5, true, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(2L);
    }
}
