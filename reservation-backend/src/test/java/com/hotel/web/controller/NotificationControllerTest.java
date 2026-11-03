package com.hotel.web.controller;

import com.hotel.application.services.InAppNotificationService;
import com.hotel.application.services.SseNotificationService;
import com.hotel.web.dto.NotificationResponse;
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
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SseNotificationService sseNotificationService;

    @Mock
    private InAppNotificationService inAppNotificationService;

    @InjectMocks
    private NotificationController notificationController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getMyNotifications_Returns200OkWithList() throws Exception {
        NotificationResponse n1 = new NotificationResponse(
                1L, "Fast-Pass Listo", "Pre-Check-in confirmado", "PRE_CHECK_IN", false, "/my-reservations", LocalDateTime.now()
        );
        when(inAppNotificationService.getMyNotifications(15)).thenReturn(List.of(n1));

        mockMvc.perform(get("/api/notifications").param("limit", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Fast-Pass Listo"))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void getUnreadCount_Returns200OkWithCount() throws Exception {
        when(inAppNotificationService.getUnreadCount()).thenReturn(5L);

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(5));
    }

    @Test
    void markAsRead_Returns200OkWithUpdatedNotification() throws Exception {
        NotificationResponse updated = new NotificationResponse(
                2L, "Comanda", "Orden entregada", "ROOM_SERVICE", true, "/my-reservations", LocalDateTime.now()
        );
        when(inAppNotificationService.markAsRead(2L)).thenReturn(updated);

        mockMvc.perform(patch("/api/notifications/2/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void markAllAsRead_Returns204NoContent() throws Exception {
        doNothing().when(inAppNotificationService).markAllAsRead();

        mockMvc.perform(patch("/api/notifications/read-all"))
                .andExpect(status().isNoContent());

        verify(inAppNotificationService, times(1)).markAllAsRead();
    }
}
