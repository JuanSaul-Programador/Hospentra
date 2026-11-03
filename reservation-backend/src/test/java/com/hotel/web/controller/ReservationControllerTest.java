package com.hotel.web.controller;

import com.hotel.application.services.ReservationService;
import com.hotel.domain.model.PaymentMethod;
import com.hotel.domain.model.PaymentStatus;
import com.hotel.domain.model.ReservationStatus;
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
                true, LocalDateTime.now(), "PASSPORT", "P123456", "Española", "14:30", "Piso alto"
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
}
