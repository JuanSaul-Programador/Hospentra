package com.hotel.web.controller;

import com.hotel.application.services.ReviewService;
import com.hotel.domain.model.Role;
import com.hotel.domain.model.User;
import com.hotel.domain.repository.UserRepository;
import com.hotel.infrastructure.security.config.SecurityConfig;
import com.hotel.infrastructure.security.jwt.JwtAuthenticationFilter;
import com.hotel.infrastructure.security.jwt.JwtService;
import com.hotel.web.dto.ManagementResponseRequest;
import com.hotel.web.dto.ReviewAdminMetricsResponse;
import com.hotel.web.dto.ReviewRequest;
import com.hotel.web.dto.ReviewResponse;
import com.hotel.web.dto.ReviewSummaryResponse;
import com.hotel.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReviewController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private UserRepository userRepository;

    private String customerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        User customer = User.builder()
                .id(1L)
                .username("pepe")
                .email("pepe@gmail.com")
                .password("encoded")
                .role(Role.ROLE_CUSTOMER)
                .enabled(true)
                .build();

        User admin = User.builder()
                .id(2L)
                .username("admin")
                .email("admin@hospentra.com")
                .password("encoded")
                .role(Role.ROLE_ADMIN)
                .enabled(true)
                .build();

        when(userRepository.findByUsername("pepe")).thenReturn(Optional.of(customer));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        customerToken = jwtService.generateToken(customer);
        adminToken = jwtService.generateToken(admin);
    }

    @Test
    void createReview_ValidPayload_Returns201Created() throws Exception {
        ReviewRequest request = ReviewRequest.builder()
                .reservationId(10L)
                .rating(5)
                .title("Estancia de lujo")
                .comment("Excelente atención en todos los aspectos.")
                .build();

        ReviewResponse response = ReviewResponse.builder()
                .id(1L)
                .reservationId(10L)
                .roomId(1L)
                .rating(5)
                .comment("Excelente atención en todos los aspectos.")
                .verifiedStay(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewService.createReview(any(ReviewRequest.class), eq("pepe"))).thenReturn(response);

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.verifiedStay").value(true));
    }

    @Test
    void createReview_RatingZero_Returns400BadRequest() throws Exception {
        ReviewRequest request = ReviewRequest.builder()
                .reservationId(10L)
                .rating(0)
                .comment("Calificación inválida menor a 1")
                .build();

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReview_RatingGreaterThanFive_Returns400BadRequest() throws Exception {
        ReviewRequest request = ReviewRequest.builder()
                .reservationId(10L)
                .rating(6)
                .comment("Calificación inválida mayor a 5")
                .build();

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReview_BlankComment_Returns400BadRequest() throws Exception {
        ReviewRequest request = ReviewRequest.builder()
                .reservationId(10L)
                .rating(5)
                .comment("   ")
                .build();

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReviewSummary_Returns200WithDistribution() throws Exception {
        ReviewSummaryResponse summary = ReviewSummaryResponse.builder()
                .roomId(1L)
                .averageRating(4.8)
                .totalReviews(5L)
                .ratingDistribution(Map.of(5, 3L, 4, 1L, 3, 1L, 2, 0L, 1, 0L))
                .ratingPercentages(Map.of(5, 60.0, 4, 20.0, 3, 20.0, 2, 0.0, 1, 0.0))
                .build();

        when(reviewService.getReviewSummary(1L)).thenReturn(summary);

        mockMvc.perform(get("/api/reviews/room/1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(1L))
                .andExpect(jsonPath("$.averageRating").value(4.8))
                .andExpect(jsonPath("$.totalReviews").value(5))
                .andExpect(jsonPath("$.ratingDistribution.5").value(3))
                .andExpect(jsonPath("$.ratingPercentages.5").value(60.0));
    }

    @Test
    void getReviewsByRoom_Returns200WithPagedContent() throws Exception {
        ReviewResponse rev = ReviewResponse.builder()
                .id(1L)
                .rating(5)
                .comment("Genial")
                .verifiedStay(true)
                .build();

        when(reviewService.getReviewsByRoom(eq(1L), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(rev), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/reviews/room/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].rating").value(5));
    }

    @Test
    void getReviewByReservation_WhenFound_Returns200() throws Exception {
        ReviewResponse rev = ReviewResponse.builder()
                .id(99L)
                .reservationId(50L)
                .rating(5)
                .verifiedStay(true)
                .build();

        when(reviewService.getReviewByReservation(50L, "pepe")).thenReturn(rev);

        mockMvc.perform(get("/api/reviews/reservation/50")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(99L))
                .andExpect(jsonPath("$.reservationId").value(50L));
    }

    @Test
    void getReviewByReservation_WhenNotFound_Returns204NoContent() throws Exception {
        when(reviewService.getReviewByReservation(999L, "pepe")).thenReturn(null);

        mockMvc.perform(get("/api/reviews/reservation/999")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void addManagementResponse_ValidPayload_Returns200() throws Exception {
        ManagementResponseRequest req = ManagementResponseRequest.builder()
                .response("Muchas gracias por visitarnos.")
                .build();

        ReviewResponse rev = ReviewResponse.builder()
                .id(1L)
                .managementResponse("Muchas gracias por visitarnos.")
                .build();

        when(reviewService.addManagementResponse(1L, "Muchas gracias por visitarnos.")).thenReturn(rev);

        mockMvc.perform(post("/api/reviews/1/management-response")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.managementResponse").value("Muchas gracias por visitarnos."));
    }

    @Test
    void addManagementResponse_BlankPayload_Returns400BadRequest() throws Exception {
        ManagementResponseRequest req = ManagementResponseRequest.builder()
                .response("   ")
                .build();

        mockMvc.perform(post("/api/reviews/1/management-response")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReviewsForAdmin_Returns200() throws Exception {
        ReviewResponse rev = ReviewResponse.builder().id(1L).rating(5).comment("Excelente").build();
        when(reviewService.getReviewsForAdmin(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(rev), PageRequest.of(0, 15), 1));

        mockMvc.perform(get("/api/reviews/admin")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L));
    }

    @Test
    void getAdminMetrics_Returns200() throws Exception {
        ReviewAdminMetricsResponse metrics = ReviewAdminMetricsResponse.builder()
                .totalReviews(15L)
                .averageRating(4.8)
                .unansweredCount(2L)
                .answeredCount(13L)
                .satisfactionRate(93.3)
                .build();

        when(reviewService.getAdminMetrics()).thenReturn(metrics);

        mockMvc.perform(get("/api/reviews/admin/metrics")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReviews").value(15))
                .andExpect(jsonPath("$.averageRating").value(4.8))
                .andExpect(jsonPath("$.unansweredCount").value(2));
    }
}
