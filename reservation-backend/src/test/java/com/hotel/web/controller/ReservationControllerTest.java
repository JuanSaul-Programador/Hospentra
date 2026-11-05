package com.hotel.web.controller;

import com.hotel.application.services.ReservationService;
import com.hotel.domain.model.PaymentMethod;
import com.hotel.domain.model.PaymentStatus;
import com.hotel.domain.model.ReservationStatus;
import com.hotel.web.dto.CheckOutSettlementRequest;
import com.hotel.web.dto.ExpressCheckOutRequest;
import com.hotel.web.dto.PreCheckInRequest;
import com.hotel.web.dto.ReservationResponse;
import com.hotel.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReservationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private ReservationController reservationController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reservationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void submitPreCheckIn_Returns200OkWithCompletedStatus() throws Exception {
        ReservationResponse response = new ReservationResponse(
                1L, null, null,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3),
                ReservationStatus.CONFIRMED, new BigDecimal("600.00"),
                LocalDateTime.now(), PaymentMethod.CREDIT_CARD, PaymentStatus.PAID,
                "4242", "pi_123", "AUTH-1",
                true, LocalDateTime.now(), "PASSPORT", "P123456", "Española", "14:30", "Piso alto",
                null, null, null, null
        );

        when(reservationService.submitPreCheckIn(eq(1L), any(PreCheckInRequest.class))).thenReturn(response);

        String jsonBody = """
                {
                    "documentType": "PASSPORT",
                    "documentNumber": "P123456",
                    "nationality": "Española",
                    "estimatedArrivalTime": "14:30",
                    "specialRequests": "Piso alto"
                }
                """;

        mockMvc.perform(patch("/api/reservations/1/pre-check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.preCheckInCompleted").value(true))
                .andExpect(jsonPath("$.documentType").value("PASSPORT"))
                .andExpect(jsonPath("$.documentNumber").value("P123456"))
                .andExpect(jsonPath("$.estimatedArrivalTime").value("14:30"));
    }

    @Test
    void submitPreCheckIn_whenMissingFields_Returns400BadRequest() throws Exception {
        String invalidJson = """
                {
                    "documentType": "",
                    "documentNumber": "",
                    "nationality": ""
                }
                """;

        mockMvc.perform(patch("/api/reservations/1/pre-check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void complete_withSettlementPayload_Returns200Ok() throws Exception {
        ReservationResponse response = new ReservationResponse(
                1L, null, null,
                LocalDate.now().minusDays(2), LocalDate.now(),
                ReservationStatus.COMPLETED, new BigDecimal("1000.00"),
                LocalDateTime.now().minusDays(2), PaymentMethod.CREDIT_CARD, PaymentStatus.PAID,
                "4242", "pi_123", "AUTH-POS-99",
                true, LocalDateTime.now().minusDays(2), "PASSPORT", "P123456", "Española", "15:00", "Piso alto",
                "POS_TERMINAL", new BigDecimal("670.00"), "Liquidado mostrador", LocalDateTime.now()
        );

        when(reservationService.complete(eq(1L), any(CheckOutSettlementRequest.class))).thenReturn(response);

        String jsonBody = """
                {
                    "settlementMethod": "POS_TERMINAL",
                    "settlementAmount": 670.00,
                    "authCode": "AUTH-POS-99",
                    "notes": "Liquidado mostrador"
                }
                """;

        mockMvc.perform(patch("/api/reservations/1/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.settlementMethod").value("POS_TERMINAL"))
                .andExpect(jsonPath("$.settlementAmount").value(670.00));
    }

    @Test
    void expressCheckOut_withValidRequest_Returns200Ok() throws Exception {
        ReservationResponse response = new ReservationResponse(
                1L, null, null,
                LocalDate.now().minusDays(2), LocalDate.now(),
                ReservationStatus.COMPLETED, new BigDecimal("1000.00"),
                LocalDateTime.now().minusDays(2), PaymentMethod.CREDIT_CARD, PaymentStatus.PAID,
                "4242", "pi_exp_888", "AUTH-1",
                true, LocalDateTime.now().minusDays(2), "PASSPORT", "P123456", "Española", "15:00", "Piso alto",
                "ONLINE_EXPRESS", new BigDecimal("670.00"), "Check-Out Express Digital", LocalDateTime.now()
        );

        when(reservationService.expressCheckOut(eq(1L), any(ExpressCheckOutRequest.class))).thenReturn(response);

        String jsonBody = """
                {
                    "amount": 670.00,
                    "cardLast4": "4242",
                    "paymentIntentId": "pi_exp_888",
                    "notes": "Check-Out Express Digital"
                }
                """;

        mockMvc.perform(post("/api/reservations/1/express-checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.settlementMethod").value("ONLINE_EXPRESS"))
                .andExpect(jsonPath("$.settlementAmount").value(670.00));
    }

    @Test
    void expressCheckOut_withMissingCardLast4_Returns400BadRequest() throws Exception {
        String invalidJson = """
                {
                    "amount": 670.00,
                    "cardLast4": ""
                }
                """;

        mockMvc.perform(post("/api/reservations/1/express-checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
