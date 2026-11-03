package com.hotel.application.services;

import com.hotel.application.dto.notification.NotificationEvent;
import com.hotel.application.services.impl.InAppNotificationServiceImpl;
import com.hotel.domain.model.Notification;
import com.hotel.domain.model.User;
import com.hotel.domain.repository.NotificationRepository;
import com.hotel.domain.repository.UserRepository;
import com.hotel.web.dto.NotificationResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InAppNotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SseNotificationService sseNotificationService;

    private InAppNotificationServiceImpl notificationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        notificationService = new InAppNotificationServiceImpl(
                notificationRepository,
                userRepository,
                sseNotificationService
        );

        testUser = User.builder()
                .id(1L)
                .username("carlos.guest")
                .email("carlos@example.com")
                .role(com.hotel.domain.model.Role.ROLE_CUSTOMER)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyNotifications_whenAuthenticated_shouldReturnMappedList() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("carlos.guest", "password", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
        );
        when(userRepository.findByUsername("carlos.guest")).thenReturn(Optional.of(testUser));

        Notification notif = Notification.builder()
                .id(10L)
                .user(testUser)
                .title("Fast-Pass Activado")
                .message("Pre-Check-In listo")
                .type("PRE_CHECK_IN")
                .readStatus(false)
                .link("/my-reservations")
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(notif));

        List<NotificationResponse> result = notificationService.getMyNotifications(15);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Fast-Pass Activado");
        assertThat(result.get(0).read()).isFalse();
    }

    @Test
    void getMyNotifications_whenNotAuthenticated_shouldReturnEmptyList() {
        SecurityContextHolder.clearContext();

        List<NotificationResponse> result = notificationService.getMyNotifications(15);

        assertThat(result).isEmpty();
        verifyNoInteractions(notificationRepository);
    }

    @Test
    void getUnreadCount_whenAuthenticated_shouldReturnCount() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("carlos.guest", "password", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
        );
        when(userRepository.findByUsername("carlos.guest")).thenReturn(Optional.of(testUser));
        when(notificationRepository.countByUserIdAndReadStatusFalse(1L)).thenReturn(4L);

        long count = notificationService.getUnreadCount();

        assertThat(count).isEqualTo(4L);
    }

    @Test
    void markAsRead_whenOwner_shouldUpdateAndReturn() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("carlos.guest", "password", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
        );
        when(userRepository.findByUsername("carlos.guest")).thenReturn(Optional.of(testUser));

        Notification notif = Notification.builder()
                .id(10L)
                .user(testUser)
                .title("Orden Lista")
                .message("Room service en camino")
                .type("ROOM_SERVICE")
                .readStatus(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notif));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        NotificationResponse response = notificationService.markAsRead(10L);

        assertThat(response.read()).isTrue();
        assertThat(notif.isReadStatus()).isTrue();
        verify(notificationRepository).save(notif);
    }

    @Test
    void markAsRead_whenNotOwner_shouldThrowAccessDenied() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("carlos.guest", "password", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
        );
        when(userRepository.findByUsername("carlos.guest")).thenReturn(Optional.of(testUser));

        User otherUser = User.builder().id(99L).username("other").build();
        Notification notif = Notification.builder()
                .id(10L)
                .user(otherUser)
                .title("Alerta")
                .build();

        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notif));

        assertThatThrownBy(() -> notificationService.markAsRead(10L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No tiene permiso");
    }

    @Test
    void markAllAsRead_whenAuthenticated_shouldInvokeRepository() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("carlos.guest", "password", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
        );
        when(userRepository.findByUsername("carlos.guest")).thenReturn(Optional.of(testUser));

        notificationService.markAllAsRead();

        verify(notificationRepository).markAllAsReadByUserId(1L);
    }

    @Test
    void createNotification_shouldSaveAndBroadcastViaSse() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            n.setId(100L);
            return n;
        });

        NotificationResponse response = notificationService.createNotification(
                testUser,
                "Reserva Confirmada",
                "Su suite está reservada",
                "RESERVATION",
                "/my-reservations"
        );

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.title()).isEqualTo("Reserva Confirmada");
        assertThat(response.read()).isFalse();

        verify(sseNotificationService).sendToUser(eq("carlos.guest"), any(NotificationEvent.class));
    }
}
