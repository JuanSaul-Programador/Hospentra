package com.hotel.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.application.services.ConciergeChatService;
import com.hotel.web.dto.ConciergeConversationSummary;
import com.hotel.web.dto.ConciergeMessageRequest;
import com.hotel.web.dto.ConciergeMessageResponse;
import com.hotel.web.dto.ConciergeStatusResponse;
import com.hotel.web.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ConciergeChatControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ConciergeChatService conciergeChatService;

    @InjectMocks
    private ConciergeChatController conciergeChatController;

    private ObjectMapper objectMapper;

    private Principal guestPrincipal;
    private Principal staffPrincipal;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(conciergeChatController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        guestPrincipal = new UsernamePasswordAuthenticationToken(
                "palo.guest", "password", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );

        staffPrincipal = new UsernamePasswordAuthenticationToken(
                "lucia.reception", "password", List.of(new SimpleGrantedAuthority("ROLE_RECEPTIONIST"))
        );
    }

    @Test
    void getStatus_Returns200Ok() throws Exception {
        ConciergeStatusResponse statusRes = new ConciergeStatusResponse(true, "101", 50L, "Palo Peros");

        when(conciergeChatService.getConciergeStatus("palo.guest")).thenReturn(statusRes);

        mockMvc.perform(get("/api/concierge/status").principal(guestPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeStay").value(true))
                .andExpect(jsonPath("$.suiteNumber").value("101"))
                .andExpect(jsonPath("$.reservationId").value(50L))
                .andExpect(jsonPath("$.guestName").value("Palo Peros"));
    }

    @Test
    void sendMessage_Returns201Created() throws Exception {
        ConciergeMessageRequest req = new ConciergeMessageRequest("¿Horario de piscina?", null, "101", null);
        ConciergeMessageResponse res = new ConciergeMessageResponse(
                1L, 10L, "Palo Peros", "ROLE_CUSTOMER", null, null, "101", "¿Horario de piscina?", false, LocalDateTime.now()
        );

        when(conciergeChatService.sendMessage(any(ConciergeMessageRequest.class), eq("palo.guest"))).thenReturn(res);

        mockMvc.perform(post("/api/concierge/messages")
                        .principal(guestPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.message").value("¿Horario de piscina?"))
                .andExpect(jsonPath("$.suiteNumber").value("101"));
    }

    @Test
    void getMyConversation_Returns200OkWithList() throws Exception {
        ConciergeMessageResponse msg = new ConciergeMessageResponse(
                1L, 10L, "Palo Peros", "ROLE_CUSTOMER", null, null, "101", "Hola", true, LocalDateTime.now()
        );

        when(conciergeChatService.getMyConversation("palo.guest")).thenReturn(List.of(msg));

        mockMvc.perform(get("/api/concierge/my-messages").principal(guestPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].message").value("Hola"));
    }

    @Test
    void getActiveConversations_Returns200OkWithList() throws Exception {
        ConciergeConversationSummary summary = new ConciergeConversationSummary(
                10L, "Palo Peros", "palo@example.com", "101", "¿Horario de spa?", LocalDateTime.now(), 1L, "lucia.reception", 50L
        );

        when(conciergeChatService.getActiveConversations("lucia.reception")).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/concierge/conversations").principal(staffPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].guestUserId").value(10L))
                .andExpect(jsonPath("$[0].guestName").value("Palo Peros"))
                .andExpect(jsonPath("$[0].suiteNumber").value("101"))
                .andExpect(jsonPath("$[0].unreadCount").value(1L))
                .andExpect(jsonPath("$[0].claimedBy").value("lucia.reception"))
                .andExpect(jsonPath("$[0].reservationId").value(50L));
    }

    @Test
    void getConversationWithGuest_Returns200OkWithList() throws Exception {
        ConciergeMessageResponse msg = new ConciergeMessageResponse(
                2L, 10L, "Palo Peros", "ROLE_CUSTOMER", null, null, "101", "¿Horario de spa?", false, LocalDateTime.now()
        );

        when(conciergeChatService.getConversationWithGuest(10L, "lucia.reception")).thenReturn(List.of(msg));

        mockMvc.perform(get("/api/concierge/conversations/10").principal(staffPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2L))
                .andExpect(jsonPath("$[0].message").value("¿Horario de spa?"));
    }

    @Test
    void claimConversation_Returns200Ok() throws Exception {
        ConciergeConversationSummary summary = new ConciergeConversationSummary(
                10L, "Palo Peros", "palo@example.com", "101", "Asignado", LocalDateTime.now(), 0L, "lucia.reception", 50L
        );

        when(conciergeChatService.claimConversation(10L, "lucia.reception")).thenReturn(summary);

        mockMvc.perform(patch("/api/concierge/conversations/10/claim").principal(staffPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guestUserId").value(10L))
                .andExpect(jsonPath("$.claimedBy").value("lucia.reception"));
    }

    @Test
    void markAsRead_Returns204NoContent() throws Exception {
        mockMvc.perform(patch("/api/concierge/conversations/10/read").principal(staffPrincipal))
                .andExpect(status().isNoContent());

        verify(conciergeChatService).markConversationAsRead(10L, "lucia.reception");
    }

    @Test
    void getUnreadCount_Returns200Ok() throws Exception {
        when(conciergeChatService.getUnreadCount("lucia.reception")).thenReturn(3L);

        mockMvc.perform(get("/api/concierge/unread-count").principal(staffPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(3L));
    }
}
