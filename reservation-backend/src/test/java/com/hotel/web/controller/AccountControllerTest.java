package com.hotel.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.application.services.AccountService;
import com.hotel.domain.model.Role;
import com.hotel.web.dto.*;
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

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(accountController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getProfile_Returns200Ok() throws Exception {
        UserProfileResponse response = new UserProfileResponse(
                1L, "lucia", "lucia@hotel.com", Role.ROLE_CUSTOMER, true,
                "Lucia", "Perez", "5551234", null, null, false
        );
        when(accountService.getProfile("lucia")).thenReturn(response);

        mockMvc.perform(get("/api/account/profile")
                        .principal(() -> "lucia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("lucia"))
                .andExpect(jsonPath("$.firstName").value("Lucia"));
    }

    @Test
    void updateProfile_ValidRequest_Returns200Ok() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Lucia", "Perez", "5559999");
        UserProfileResponse response = new UserProfileResponse(
                1L, "lucia", "lucia@hotel.com", Role.ROLE_CUSTOMER, true,
                "Lucia", "Perez", "5559999", null, null, false
        );
        when(accountService.updateProfile(eq("lucia"), any(UpdateProfileRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/account/profile")
                        .principal(() -> "lucia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("5559999"));
    }

    @Test
    void updateProfile_InvalidRequest_Returns400BadRequest() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("", "", null);

        mockMvc.perform(put("/api/account/profile")
                        .principal(() -> "lucia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestPasswordChangeOtp_Returns200Ok() throws Exception {
        mockMvc.perform(post("/api/account/change-password/request-otp")
                        .principal(() -> "lucia"))
                .andExpect(status().isOk());
    }

    @Test
    void changePassword_ValidRequest_Returns200Ok() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("123456", "newPassword123");

        mockMvc.perform(post("/api/account/change-password")
                        .principal(() -> "lucia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void changePassword_InvalidOtp_Returns409Conflict() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("999999", "newPassword123");
        doThrow(new IllegalArgumentException("Código de verificación incorrecto o inexistente."))
                .when(accountService).changePassword(eq("lucia"), any(ChangePasswordRequest.class));

        mockMvc.perform(post("/api/account/change-password")
                        .principal(() -> "lucia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Código de verificación incorrecto o inexistente."));
    }

    @Test
    void requestAccountDeletion_ValidRequest_Returns200Ok() throws Exception {
        RequestAccountDeletionRequest request = new RequestAccountDeletionRequest("secret123", "Mudanza");
        AccountDeletionStatusResponse response = new AccountDeletionStatusResponse(
                true, LocalDateTime.now(), LocalDateTime.now().plusDays(30), "Programada"
        );
        when(accountService.requestAccountDeletion(eq("lucia"), any(RequestAccountDeletionRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/account/request-deletion")
                        .principal(() -> "lucia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingDeletion").value(true));
    }

    @Test
    void requestAccountDeletion_ActiveReservations_Returns400BadRequest() throws Exception {
        RequestAccountDeletionRequest request = new RequestAccountDeletionRequest("secret123", "Mudanza");
        when(accountService.requestAccountDeletion(eq("lucia"), any(RequestAccountDeletionRequest.class)))
                .thenThrow(new IllegalStateException("No puede solicitar la eliminación de su cuenta mientras tenga reservaciones activas"));

        mockMvc.perform(post("/api/account/request-deletion")
                        .principal(() -> "lucia")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("No puede solicitar la eliminación de su cuenta mientras tenga reservaciones activas"));
    }

    @Test
    void cancelAccountDeletion_Returns200Ok() throws Exception {
        AccountDeletionStatusResponse response = new AccountDeletionStatusResponse(
                false, null, null, "Cancelada"
        );
        when(accountService.cancelAccountDeletion("lucia")).thenReturn(response);

        mockMvc.perform(post("/api/account/cancel-deletion")
                        .principal(() -> "lucia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingDeletion").value(false));
    }
}
