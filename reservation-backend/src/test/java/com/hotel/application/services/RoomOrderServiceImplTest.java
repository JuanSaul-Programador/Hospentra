package com.hotel.application.services;

import com.hotel.application.dto.notification.NotificationEvent;
import com.hotel.application.services.impl.RoomOrderServiceImpl;
import com.hotel.domain.model.*;
import com.hotel.domain.repository.ExtraServiceRepository;
import com.hotel.domain.repository.ReservationRepository;
import com.hotel.domain.repository.RoomOrderRepository;
import com.hotel.domain.repository.TherapistRepository;
import com.hotel.domain.repository.UserRepository;
import com.hotel.web.dto.CreateOrderItemRequest;
import com.hotel.web.dto.CreateRoomOrderRequest;
import com.hotel.web.dto.RoomOrderResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomOrderServiceImplTest {

    @Mock
    private RoomOrderRepository roomOrderRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ExtraServiceRepository extraServiceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TherapistRepository therapistRepository;

    @Mock
    private SseNotificationService sseNotificationService;

    @Mock
    private com.hotel.application.services.InAppNotificationService inAppNotificationService;

    private RoomOrderServiceImpl roomOrderService;

    private Reservation standardReservation;
    private ExtraService standardSteak;
    private Therapist activeTherapist;

    @BeforeEach
    void setUp() {
        roomOrderService = new RoomOrderServiceImpl(
                roomOrderRepository,
                reservationRepository,
                extraServiceRepository,
                userRepository,
                therapistRepository,
                sseNotificationService,
                inAppNotificationService
        );

        Guest guest = Guest.builder()
                .id(1L)
                .firstName("Elena")
                .lastName("Rostova")
                .build();

        Room room = Room.builder()
                .id(102L)
                .roomNumber("102")
                .status(RoomStatus.OCCUPIED)
                .build();

        standardReservation = Reservation.builder()
                .id(77L)
                .guest(guest)
                .room(room)
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(2))
                .status(ReservationStatus.CHECKED_IN)
                .build();

        standardSteak = ExtraService.builder()
                .id(10L)
                .name("Solomillo de Autor al Malbec")
                .category(ExtraCategory.GASTRONOMY)
                .price(new BigDecimal("48.00"))
                .available(true)
                .build();

        activeTherapist = Therapist.builder()
                .id(5L)
                .name("Camila Valenzuela")
                .specialty("Aromaterapia & Masaje Sueco")
                .phone("+51912345678")
                .active(true)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRoomOrderSuccessCalculatesSubtotalsAndBroadcasts() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("recep", null, List.of(new SimpleGrantedAuthority("ROLE_RECEPTIONIST")))
        );

        CreateRoomOrderRequest request = new CreateRoomOrderRequest(
                77L,
                "Sin sal",
                "IN_SUITE",
                null,
                List.of(new CreateOrderItemRequest(10L, 2))
        );

        when(reservationRepository.findById(77L)).thenReturn(Optional.of(standardReservation));
        when(extraServiceRepository.findById(10L)).thenReturn(Optional.of(standardSteak));

        when(roomOrderRepository.save(any(RoomOrder.class))).thenAnswer(invocation -> {
            RoomOrder ro = invocation.getArgument(0);
            ro.setId(201L);
            ro.setOrderedAt(LocalDateTime.now());
            return ro;
        });

        RoomOrderResponse response = roomOrderService.createOrder(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(201L);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("96.00"));
        assertThat(response.status()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).subtotal()).isEqualByComparingTo(new BigDecimal("96.00"));

        verify(sseNotificationService, times(1)).sendToRole(eq("ROLE_ADMIN"), any(NotificationEvent.class));
        verify(sseNotificationService, times(1)).sendToRole(eq("ROLE_RECEPTIONIST"), any(NotificationEvent.class));
        verify(sseNotificationService, times(1)).sendToRole(eq("ROLE_KITCHEN"), any(NotificationEvent.class));
        verify(sseNotificationService, never()).broadcast(any(NotificationEvent.class));
    }

    @Test
    void createRoomOrderWhenUnauthenticatedThrowsAccessDeniedException() {
        CreateRoomOrderRequest request = new CreateRoomOrderRequest(
                77L,
                "Sin sal",
                "IN_SUITE",
                null,
                List.of(new CreateOrderItemRequest(10L, 1))
        );
        when(reservationRepository.findById(77L)).thenReturn(Optional.of(standardReservation));

        assertThatThrownBy(() -> roomOrderService.createOrder(request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateOrderStatusTransitionsToPreparingAndReady() {
        RoomOrder order = RoomOrder.builder()
                .id(201L)
                .reservationId(77L)
                .roomNumber("102")
                .guestName("Elena Rostova")
                .totalAmount(new BigDecimal("48.00"))
                .status(OrderStatus.RECEIVED)
                .items(new ArrayList<>())
                .build();

        when(roomOrderRepository.findById(201L)).thenReturn(Optional.of(order));
        when(roomOrderRepository.save(any(RoomOrder.class))).thenAnswer(i -> i.getArgument(0));

        RoomOrderResponse response = roomOrderService.updateOrderStatus(201L, OrderStatus.PREPARING);

        assertThat(response.status()).isEqualTo(OrderStatus.PREPARING);
        verify(sseNotificationService, times(1)).sendToRole(eq("ROLE_ADMIN"), any(NotificationEvent.class));
        verify(sseNotificationService, times(1)).sendToRole(eq("ROLE_RECEPTIONIST"), any(NotificationEvent.class));
        verify(sseNotificationService, times(1)).sendToRole(eq("ROLE_KITCHEN"), any(NotificationEvent.class));
        verify(sseNotificationService, never()).broadcast(any(NotificationEvent.class));
    }

    @Test
    void updateOrderStatusToDeliveredSetsDeliveredAt() {
        RoomOrder order = RoomOrder.builder()
                .id(201L)
                .reservationId(77L)
                .roomNumber("102")
                .guestName("Elena Rostova")
                .totalAmount(new BigDecimal("48.00"))
                .status(OrderStatus.READY_FOR_DELIVERY)
                .items(new ArrayList<>())
                .build();

        when(roomOrderRepository.findById(201L)).thenReturn(Optional.of(order));
        when(roomOrderRepository.save(any(RoomOrder.class))).thenAnswer(i -> i.getArgument(0));

        RoomOrderResponse response = roomOrderService.updateOrderStatus(201L, OrderStatus.DELIVERED);

        assertThat(response.status()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(response.deliveredAt()).isNotNull();
        verify(sseNotificationService, times(1)).sendToRole(eq("ROLE_ADMIN"), any(NotificationEvent.class));
        verify(sseNotificationService, times(1)).sendToRole(eq("ROLE_RECEPTIONIST"), any(NotificationEvent.class));
        verify(sseNotificationService, times(1)).sendToRole(eq("ROLE_KITCHEN"), any(NotificationEvent.class));
        verify(sseNotificationService, never()).broadcast(any(NotificationEvent.class));
    }

    @Test
    void assignTherapistSuccessAdvancesToPreparing() {
        RoomOrder order = RoomOrder.builder()
                .id(300L)
                .reservationId(77L)
                .roomNumber("102")
                .guestName("Elena Rostova")
                .scheduledTime("17:00 hrs")
                .status(OrderStatus.RECEIVED)
                .items(new ArrayList<>())
                .build();

        when(roomOrderRepository.findById(300L)).thenReturn(Optional.of(order));
        when(therapistRepository.findById(5L)).thenReturn(Optional.of(activeTherapist));
        when(roomOrderRepository.existsByTherapistIdAndScheduledTimeAndStatusIn(eq(5L), eq("17:00 hrs"), anyList()))
                .thenReturn(false);
        when(roomOrderRepository.save(any(RoomOrder.class))).thenAnswer(i -> i.getArgument(0));

        RoomOrderResponse response = roomOrderService.assignTherapist(300L, 5L);

        assertThat(response.therapistId()).isEqualTo(5L);
        assertThat(response.therapistName()).isEqualTo("Camila Valenzuela");
        assertThat(response.status()).isEqualTo(OrderStatus.PREPARING);
        verify(sseNotificationService, times(1)).sendToRole(eq("ROLE_ADMIN"), any(NotificationEvent.class));
        verify(sseNotificationService, times(1)).sendToRole(eq("ROLE_RECEPTIONIST"), any(NotificationEvent.class));
        verify(sseNotificationService, never()).broadcast(any(NotificationEvent.class));
    }

    @Test
    void assignTherapistFailsWhenTherapistInactive() {
        activeTherapist.setActive(false);

        RoomOrder order = RoomOrder.builder()
                .id(300L)
                .status(OrderStatus.RECEIVED)
                .build();

        when(roomOrderRepository.findById(300L)).thenReturn(Optional.of(order));
        when(therapistRepository.findById(5L)).thenReturn(Optional.of(activeTherapist));

        assertThatThrownBy(() -> roomOrderService.assignTherapist(300L, 5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inactivo o en descanso");
    }

    @Test
    void assignTherapistFailsWhenScheduleConflict() {
        RoomOrder order = RoomOrder.builder()
                .id(300L)
                .scheduledTime("17:00 hrs")
                .status(OrderStatus.RECEIVED)
                .build();

        when(roomOrderRepository.findById(300L)).thenReturn(Optional.of(order));
        when(therapistRepository.findById(5L)).thenReturn(Optional.of(activeTherapist));
        when(roomOrderRepository.existsByTherapistIdAndScheduledTimeAndStatusIn(eq(5L), eq("17:00 hrs"), anyList()))
                .thenReturn(true);

        assertThatThrownBy(() -> roomOrderService.assignTherapist(300L, 5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya tiene una cita asignada");
    }

    @Test
    void findBusyTherapistIdsReturnsList() {
        when(roomOrderRepository.findBusyTherapistIdsByScheduledTime(eq("17:00 hrs"), anyList()))
                .thenReturn(List.of(5L, 8L));

        List<Long> busyIds = roomOrderService.findBusyTherapistIds("17:00 hrs");

        assertThat(busyIds).containsExactly(5L, 8L);
    }

    @Test
    void findBusyTherapistIdsReturnsEmptyWhenScheduledTimeBlank() {
        List<Long> busyIds = roomOrderService.findBusyTherapistIds("");

        assertThat(busyIds).isEmpty();
        verifyNoInteractions(roomOrderRepository);
    }
}
