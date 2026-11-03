package com.hotel.application.services;

import com.hotel.application.dto.notification.NotificationEvent;
import com.hotel.application.services.impl.ConciergeChatServiceImpl;
import com.hotel.domain.model.ConciergeMessage;
import com.hotel.domain.model.Guest;
import com.hotel.domain.model.Reservation;
import com.hotel.domain.model.ReservationStatus;
import com.hotel.domain.model.Role;
import com.hotel.domain.model.Room;
import com.hotel.domain.model.User;
import com.hotel.domain.repository.ConciergeMessageRepository;
import com.hotel.domain.repository.ReservationRepository;
import com.hotel.domain.repository.UserRepository;
import com.hotel.web.dto.ConciergeConversationSummary;
import com.hotel.web.dto.ConciergeMessageRequest;
import com.hotel.web.dto.ConciergeMessageResponse;
import com.hotel.web.dto.ConciergeStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConciergeChatServiceImplTest {

    @Mock
    private ConciergeMessageRepository conciergeMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private SseNotificationService sseNotificationService;

    @Mock
    private AuditLogService auditLogService;

    private ConciergeChatServiceImpl conciergeChatService;

    private User guestUser;
    private User receptionistUser;
    private Guest guest;
    private Reservation activeReservation;

    @BeforeEach
    void setUp() {
        conciergeChatService = new ConciergeChatServiceImpl(
                conciergeMessageRepository,
                userRepository,
                reservationRepository,
                sseNotificationService,
                auditLogService
        );

        guest = Guest.builder()
                .id(1L)
                .firstName("Palo")
                .lastName("Peros")
                .email("palo@example.com")
                .build();

        guestUser = User.builder()
                .id(10L)
                .username("palo.guest")
                .email("palo@example.com")
                .role(Role.ROLE_CUSTOMER)
                .guest(guest)
                .build();

        receptionistUser = User.builder()
                .id(20L)
                .username("lucia.reception")
                .email("lucia@hospentra.com")
                .role(Role.ROLE_RECEPTIONIST)
                .build();

        Room room = Room.builder()
                .id(200L)
                .roomNumber("101")
                .build();

        activeReservation = Reservation.builder()
                .id(50L)
                .guest(guest)
                .room(room)
                .status(ReservationStatus.CHECKED_IN)
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();
    }

    @Test
    void sendMessage_fromCustomer_withActiveStay_shouldSaveAndBroadcastToFrontDesk() {
        when(userRepository.findByUsername("palo.guest")).thenReturn(Optional.of(guestUser));
        when(reservationRepository.findByGuestId(1L)).thenReturn(List.of(activeReservation));

        ConciergeMessage saved = ConciergeMessage.builder()
                .id(100L)
                .sender(guestUser)
                .recipient(null)
                .reservation(activeReservation)
                .senderRole(Role.ROLE_CUSTOMER)
                .senderName("Palo Peros")
                .suiteNumber("101")
                .message("¿Tienen toallas extras?")
                .readStatus(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(conciergeMessageRepository.save(any(ConciergeMessage.class))).thenReturn(saved);

        ConciergeMessageRequest request = new ConciergeMessageRequest(
                "¿Tienen toallas extras?",
                null,
                "101",
                null
        );

        ConciergeMessageResponse result = conciergeChatService.sendMessage(request, "palo.guest");

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.message()).isEqualTo("¿Tienen toallas extras?");
        assertThat(result.suiteNumber()).isEqualTo("101");
        assertThat(result.senderName()).isEqualTo("Palo Peros");

        verify(sseNotificationService).sendToRole(eq("ROLE_RECEPTIONIST"), any(NotificationEvent.class));
        verify(sseNotificationService).sendToRole(eq("ROLE_ADMIN"), any(NotificationEvent.class));
        verify(auditLogService).logAction(eq("CONCIERGE_MESSAGE"), eq("ConciergeMessage"), eq("100"), anyString(), eq("SUCCESS"));
    }

    @Test
    void sendMessage_fromCustomer_withoutActiveStay_shouldThrowAccessDenied() {
        when(userRepository.findByUsername("palo.guest")).thenReturn(Optional.of(guestUser));
        when(reservationRepository.findByGuestId(1L)).thenReturn(List.of());

        ConciergeMessageRequest request = new ConciergeMessageRequest(
                "¿Tienen toallas extras?",
                null,
                "101",
                null
        );

        assertThatThrownBy(() -> conciergeChatService.sendMessage(request, "palo.guest"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("El Concierge de Suite es exclusivo para huéspedes con estancia activa");
    }

    @Test
    void sendMessage_fromReceptionist_shouldSaveAndSendToGuest() {
        when(userRepository.findByUsername("lucia.reception")).thenReturn(Optional.of(receptionistUser));
        when(userRepository.findById(10L)).thenReturn(Optional.of(guestUser));
        when(reservationRepository.findByGuestId(1L)).thenReturn(List.of(activeReservation));

        ConciergeMessage saved = ConciergeMessage.builder()
                .id(101L)
                .sender(receptionistUser)
                .recipient(guestUser)
                .reservation(activeReservation)
                .senderRole(Role.ROLE_RECEPTIONIST)
                .senderName("lucia.reception")
                .suiteNumber("101")
                .message("Con gusto, ya van en camino.")
                .readStatus(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(conciergeMessageRepository.save(any(ConciergeMessage.class))).thenReturn(saved);

        ConciergeMessageRequest request = new ConciergeMessageRequest(
                "Con gusto, ya van en camino.",
                null,
                "101",
                10L
        );

        ConciergeMessageResponse result = conciergeChatService.sendMessage(request, "lucia.reception");

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(101L);
        assertThat(result.recipientId()).isEqualTo(10L);

        verify(sseNotificationService).sendToUser(eq("palo.guest"), any(NotificationEvent.class));
        verify(auditLogService).logAction(eq("CONCIERGE_REPLY"), eq("ConciergeMessage"), eq("101"), anyString(), eq("SUCCESS"));
    }

    @Test
    void sendMessage_fromStaff_whenRecipientNull_shouldThrowIllegalArgument() {
        when(userRepository.findByUsername("lucia.reception")).thenReturn(Optional.of(receptionistUser));

        ConciergeMessageRequest request = new ConciergeMessageRequest("Mensaje sin destinatario", null, "101", null);

        assertThatThrownBy(() -> conciergeChatService.sendMessage(request, "lucia.reception"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El personal debe especificar el ID del huésped destinatario");
    }

    @Test
    void getMyConversation_whenActiveStay_shouldReturnMessages() {
        when(userRepository.findByUsername("palo.guest")).thenReturn(Optional.of(guestUser));
        when(reservationRepository.findByGuestId(1L)).thenReturn(List.of(activeReservation));

        ConciergeMessage msg = ConciergeMessage.builder()
                .id(1L)
                .sender(guestUser)
                .senderRole(Role.ROLE_CUSTOMER)
                .senderName("Palo Peros")
                .message("Hola")
                .readStatus(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(conciergeMessageRepository.findConversationByReservationId(50L)).thenReturn(List.of(msg));

        List<ConciergeMessageResponse> result = conciergeChatService.getMyConversation("palo.guest");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).message()).isEqualTo("Hola");
    }

    @Test
    void getMyConversation_whenNoActiveStay_shouldReturnEmptyList() {
        when(userRepository.findByUsername("palo.guest")).thenReturn(Optional.of(guestUser));
        when(reservationRepository.findByGuestId(1L)).thenReturn(List.of());

        List<ConciergeMessageResponse> result = conciergeChatService.getMyConversation("palo.guest");

        assertThat(result).isEmpty();
    }

    @Test
    void getConversationWithGuest_whenStaff_shouldReturnMessages() {
        when(userRepository.findByUsername("lucia.reception")).thenReturn(Optional.of(receptionistUser));
        when(userRepository.findById(10L)).thenReturn(Optional.of(guestUser));
        when(reservationRepository.findByGuestId(1L)).thenReturn(List.of(activeReservation));

        ConciergeMessage msg = ConciergeMessage.builder()
                .id(2L)
                .sender(guestUser)
                .senderRole(Role.ROLE_CUSTOMER)
                .senderName("Palo Peros")
                .message("¿Horario de spa?")
                .readStatus(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(conciergeMessageRepository.findConversationByReservationId(50L)).thenReturn(List.of(msg));

        List<ConciergeMessageResponse> result = conciergeChatService.getConversationWithGuest(10L, "lucia.reception");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).message()).isEqualTo("¿Horario de spa?");
    }

    @Test
    void getConversationWithGuest_whenCustomer_shouldThrowAccessDenied() {
        when(userRepository.findByUsername("palo.guest")).thenReturn(Optional.of(guestUser));

        assertThatThrownBy(() -> conciergeChatService.getConversationWithGuest(10L, "palo.guest"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Acceso restringido");
    }

    @Test
    void getActiveConversations_whenStaff_shouldReturnSummariesForInHouseReservations() {
        when(userRepository.findByUsername("lucia.reception")).thenReturn(Optional.of(receptionistUser));
        when(reservationRepository.findByStatus(ReservationStatus.CHECKED_IN)).thenReturn(List.of(activeReservation));
        when(userRepository.findByGuestId(1L)).thenReturn(Optional.of(guestUser));

        ConciergeMessage msg = ConciergeMessage.builder()
                .id(3L)
                .sender(guestUser)
                .senderRole(Role.ROLE_CUSTOMER)
                .senderName("Palo Peros")
                .suiteNumber("101")
                .message("¿Puedo pedir Late Check-Out?")
                .readStatus(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(conciergeMessageRepository.findConversationByReservationId(50L)).thenReturn(List.of(msg));

        List<ConciergeConversationSummary> summaries = conciergeChatService.getActiveConversations("lucia.reception");

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).guestUserId()).isEqualTo(10L);
        assertThat(summaries.get(0).guestName()).isEqualTo("Palo Peros");
        assertThat(summaries.get(0).suiteNumber()).isEqualTo("101");
        assertThat(summaries.get(0).unreadCount()).isEqualTo(1L);
        assertThat(summaries.get(0).lastMessage()).isEqualTo("¿Puedo pedir Late Check-Out?");
    }

    @Test
    void markConversationAsRead_forCustomer_and_forStaff() {
        when(userRepository.findByUsername("palo.guest")).thenReturn(Optional.of(guestUser));
        conciergeChatService.markConversationAsRead(null, "palo.guest");
        verify(conciergeMessageRepository).markAllAsReadForRecipient(10L);

        when(userRepository.findByUsername("lucia.reception")).thenReturn(Optional.of(receptionistUser));
        conciergeChatService.markConversationAsRead(10L, "lucia.reception");
        verify(conciergeMessageRepository).markAllAsReadFromGuest(10L);
    }

    @Test
    void getUnreadCount_forCustomer_and_forStaff() {
        when(userRepository.findByUsername("palo.guest")).thenReturn(Optional.of(guestUser));
        when(conciergeMessageRepository.countByRecipientIdAndReadStatusFalse(10L)).thenReturn(2L);
        long countGuest = conciergeChatService.getUnreadCount("palo.guest");
        assertThat(countGuest).isEqualTo(2L);

        when(userRepository.findByUsername("lucia.reception")).thenReturn(Optional.of(receptionistUser));
        when(conciergeMessageRepository.countUnreadForFrontDesk()).thenReturn(5L);
        long countStaff = conciergeChatService.getUnreadCount("lucia.reception");
        assertThat(countStaff).isEqualTo(5L);
    }

    @Test
    void getConciergeStatus_forCustomerWithActiveStay_shouldReturnTrue() {
        when(userRepository.findByUsername("palo.guest")).thenReturn(Optional.of(guestUser));
        when(reservationRepository.findByGuestId(1L)).thenReturn(List.of(activeReservation));

        ConciergeStatusResponse response = conciergeChatService.getConciergeStatus("palo.guest");

        assertThat(response.activeStay()).isTrue();
        assertThat(response.suiteNumber()).isEqualTo("101");
        assertThat(response.reservationId()).isEqualTo(50L);
        assertThat(response.guestName()).isEqualTo("Palo Peros");
    }

    @Test
    void getConciergeStatus_forCustomerWithoutActiveStay_shouldReturnFalse() {
        when(userRepository.findByUsername("palo.guest")).thenReturn(Optional.of(guestUser));
        when(reservationRepository.findByGuestId(1L)).thenReturn(List.of());

        ConciergeStatusResponse response = conciergeChatService.getConciergeStatus("palo.guest");

        assertThat(response.activeStay()).isFalse();
        assertThat(response.suiteNumber()).isNull();
        assertThat(response.reservationId()).isNull();
    }

    @Test
    void claimConversation_byStaff_shouldAssignAndBroadcastClaim() {
        when(userRepository.findByUsername("lucia.reception")).thenReturn(Optional.of(receptionistUser));
        when(userRepository.findById(10L)).thenReturn(Optional.of(guestUser));
        when(reservationRepository.findByGuestId(1L)).thenReturn(List.of(activeReservation));

        ConciergeConversationSummary result = conciergeChatService.claimConversation(10L, "lucia.reception");

        assertThat(result).isNotNull();
        assertThat(result.guestUserId()).isEqualTo(10L);
        assertThat(result.claimedBy()).isEqualTo("lucia.reception");
        assertThat(result.suiteNumber()).isEqualTo("101");

        verify(sseNotificationService).sendToRole(eq("ROLE_RECEPTIONIST"), any(NotificationEvent.class));
        verify(sseNotificationService).sendToRole(eq("ROLE_ADMIN"), any(NotificationEvent.class));
        verify(auditLogService).logAction(eq("CONCIERGE_CLAIM"), eq("User"), eq("10"), anyString(), eq("SUCCESS"));
    }
}
