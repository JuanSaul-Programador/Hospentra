package com.hotel.web.security;

import com.hotel.application.services.AuthService;
import com.hotel.application.services.ReservationService;
import com.hotel.application.services.ReviewService;
import com.hotel.application.services.RoomOrderService;
import com.hotel.application.services.UserService;
import com.hotel.domain.model.PaymentMethod;
import com.hotel.domain.model.Role;
import com.hotel.domain.model.User;
import com.hotel.domain.model.WorkStatus;
import com.hotel.domain.repository.UserRepository;
import com.hotel.infrastructure.security.config.SecurityConfig;
import com.hotel.infrastructure.security.jwt.JwtAuthenticationFilter;
import com.hotel.infrastructure.security.jwt.JwtService;
import com.hotel.web.controller.AuthController;
import com.hotel.web.controller.ReservationController;
import com.hotel.web.controller.ReviewController;
import com.hotel.web.controller.RoomOrderController;
import com.hotel.web.controller.UserController;
import com.hotel.web.dto.AssignTherapistRequest;
import com.hotel.web.dto.RefreshTokenRequest;
import com.hotel.web.dto.ReservationRequest;
import com.hotel.web.dto.ReviewResponse;
import com.hotel.web.dto.RoomOrderResponse;
import com.hotel.web.dto.UpdateWorkStatusRequest;
import com.hotel.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        RoomOrderController.class,
        ReviewController.class,
        ReservationController.class,
        UserController.class,
        AuthController.class
})
@Import({SecurityConfig.class, JwtService.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class SecurityRbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RoomOrderService roomOrderService;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthService authService;

    private String adminToken;
    private String receptionistToken;
    private String kitchenToken;
    private String customerToken;
    private String disabledCustomerToken;

    @BeforeEach
    void setUp() {
        User admin = buildUser(1L, "admin", Role.ROLE_ADMIN, true);
        User receptionist = buildUser(2L, "recep", Role.ROLE_RECEPTIONIST, true);
        User kitchen = buildUser(3L, "kitchen", Role.ROLE_KITCHEN, true);
        User customer = buildUser(4L, "customer", Role.ROLE_CUSTOMER, true);
        User disabledUser = buildUser(5L, "disabled", Role.ROLE_CUSTOMER, false);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findByUsername("recep")).thenReturn(Optional.of(receptionist));
        when(userRepository.findByUsername("kitchen")).thenReturn(Optional.of(kitchen));
        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
        when(userRepository.findByUsername("disabled")).thenReturn(Optional.of(disabledUser));

        adminToken = jwtService.generateToken(admin);
        receptionistToken = jwtService.generateToken(receptionist);
        kitchenToken = jwtService.generateToken(kitchen);
        customerToken = jwtService.generateToken(customer);
        disabledCustomerToken = jwtService.generateToken(disabledUser);
    }

    private User buildUser(Long id, String username, Role role, boolean enabled) {
        return User.builder()
                .id(id)
                .username(username)
                .email(username + "@hospentra.com")
                .password("encoded")
                .role(role)
                .enabled(enabled)
                .build();
    }

    @Test
    void getAllRoomOrders_Unauthenticated_Returns403() throws Exception {
        mockMvc.perform(get("/api/room-orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllRoomOrders_AsCustomer_Returns403() throws Exception {
        mockMvc.perform(get("/api/room-orders")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllRoomOrders_AsKitchen_Returns403() throws Exception {
        mockMvc.perform(get("/api/room-orders")
                        .header("Authorization", "Bearer " + kitchenToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllRoomOrders_AsAdmin_Returns200() throws Exception {
        when(roomOrderService.findAllOrders()).thenReturn(List.of());

        mockMvc.perform(get("/api/room-orders")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void getAllRoomOrders_AsReceptionist_Returns200() throws Exception {
        when(roomOrderService.findAllOrders()).thenReturn(List.of());

        mockMvc.perform(get("/api/room-orders")
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk());
    }

    @Test
    void getBusyTherapists_AsCustomer_Returns403() throws Exception {
        mockMvc.perform(get("/api/room-orders/busy-therapists")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getBusyTherapists_AsReceptionist_Returns200() throws Exception {
        when(roomOrderService.findBusyTherapistIds(any())).thenReturn(List.of(1L));

        mockMvc.perform(get("/api/room-orders/busy-therapists")
                        .header("Authorization", "Bearer " + receptionistToken))
                .andExpect(status().isOk());
    }

    @Test
    void assignTherapist_AsCustomer_Returns403() throws Exception {
        AssignTherapistRequest request = new AssignTherapistRequest(1L);

        mockMvc.perform(patch("/api/room-orders/1/assign-therapist")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignTherapist_AsAdmin_Returns200() throws Exception {
        AssignTherapistRequest request = new AssignTherapistRequest(1L);
        when(roomOrderService.assignTherapist(1L, 1L)).thenReturn(new RoomOrderResponse(
                1L, 10L, "101", "Guest", null, null, null, null, null, 1L, "Ana", null, null, List.of(), false
        ));

        mockMvc.perform(patch("/api/room-orders/1/assign-therapist")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void getReviewByReservation_Unauthenticated_Returns403() throws Exception {
        mockMvc.perform(get("/api/reviews/reservation/10"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getReviewByReservation_WhenServiceDeniesAccess_Returns403Not500() throws Exception {
        when(reviewService.getReviewByReservation(10L, "customer"))
                .thenThrow(new AccessDeniedException("No tiene autorización"));

        mockMvc.perform(get("/api/reviews/reservation/10")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getReviewByReservation_WhenOwner_Returns200() throws Exception {
        when(reviewService.getReviewByReservation(10L, "customer"))
                .thenReturn(ReviewResponse.builder().id(1L).reservationId(10L).rating(5).build());

        mockMvc.perform(get("/api/reviews/reservation/10")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());
    }

    @Test
    void getReviewsByRoom_Unauthenticated_Returns200() throws Exception {
        when(reviewService.getReviewsByRoom(eq(1L), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/reviews/room/1"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoint_WithDisabledUserToken_Returns403() throws Exception {
        mockMvc.perform(get("/api/reservations/my")
                        .header("Authorization", "Bearer " + disabledCustomerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateWorkStatus_AsCustomer_Returns403() throws Exception {
        UpdateWorkStatusRequest request = new UpdateWorkStatusRequest(WorkStatus.ON_BREAK);

        mockMvc.perform(patch("/api/users/4/work-status")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateWorkStatus_WhenServiceDeniesIdor_Returns403Not500() throws Exception {
        UpdateWorkStatusRequest request = new UpdateWorkStatusRequest(WorkStatus.ON_BREAK);
        when(userService.updateWorkStatus(1L, WorkStatus.ON_BREAK, "recep"))
                .thenThrow(new AccessDeniedException("No tiene permisos"));

        mockMvc.perform(patch("/api/users/1/work-status")
                        .header("Authorization", "Bearer " + receptionistToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createReservation_WhenServiceDeniesIdor_Returns403Not500() throws Exception {
        ReservationRequest request = new ReservationRequest(
                999L, 101L, LocalDate.now().plusDays(2), LocalDate.now().plusDays(4), PaymentMethod.PAY_AT_CHECKIN, null
        );
        when(reservationService.create(any(ReservationRequest.class)))
                .thenThrow(new AccessDeniedException("No tiene permisos para crear reservaciones para otro huésped"));

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshToken_WhenUserDisabled_Returns403Not500() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
        when(authService.refreshToken("refresh-token"))
                .thenThrow(new DisabledException("Account is disabled"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void login_WithMalformedJson_Returns400Not500() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad-json"))
                .andExpect(status().isBadRequest());
    }
}
