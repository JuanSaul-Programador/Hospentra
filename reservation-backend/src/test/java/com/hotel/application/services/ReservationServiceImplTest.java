package com.hotel.application.services;

import com.hotel.application.dto.notification.NotificationEvent;
import com.hotel.application.mapper.GuestMapper;
import com.hotel.application.mapper.ReservationMapper;
import com.hotel.application.mapper.RoomMapper;
import com.hotel.application.mapper.RoomTypeMapper;
import com.hotel.application.services.impl.ReservationServiceImpl;
import com.hotel.domain.exception.ResourceNotFoundException;
import com.hotel.domain.model.*;
import com.hotel.domain.repository.GuestRepository;
import com.hotel.domain.repository.MaintenanceBlockRepository;
import com.hotel.domain.repository.ReservationRepository;
import com.hotel.domain.repository.RoomOrderRepository;
import com.hotel.domain.repository.RoomRepository;
import com.hotel.domain.repository.UserRepository;
import com.hotel.web.dto.CheckOutSettlementRequest;
import com.hotel.web.dto.ExpressCheckOutRequest;
import com.hotel.web.dto.PreCheckInRequest;
import com.hotel.web.dto.ReservationRequest;
import com.hotel.web.dto.ReservationResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private GuestRepository guestRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SseNotificationService sseNotificationService;

    @Mock
    private EmailService emailService;

    @Mock
    private MaintenanceBlockRepository maintenanceBlockRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private com.hotel.application.services.InAppNotificationService inAppNotificationService;

    @Mock
    private RoomOrderRepository roomOrderRepository;

    private ReservationServiceImpl reservationService;

    private RoomType standardRoomType;
    private Room standardRoom;
    private Guest standardGuest;

    @BeforeEach
    void setUp() {
        ReservationMapper reservationMapper = new ReservationMapper(
                new GuestMapper(),
                new RoomMapper(new RoomTypeMapper())
        );

        reservationService = new ReservationServiceImpl(
                reservationRepository,
                guestRepository,
                roomRepository,
                userRepository,
                maintenanceBlockRepository,
                reservationMapper,
                sseNotificationService,
                emailService,
                auditLogService,
                inAppNotificationService,
                roomOrderRepository
        );

        standardRoomType = RoomType.builder()
                .id(1L)
                .name("Suite Imperial")
                .capacity(2)
                .pricePerNight(new BigDecimal("300.00"))
                .build();

        standardRoom = Room.builder()
                .id(101L)
                .roomNumber("101")
                .status(RoomStatus.AVAILABLE)
                .roomType(standardRoomType)
                .build();

        standardGuest = Guest.builder()
                .id(10L)
                .firstName("Carlos")
                .lastName("Mendoza")
                .email("carlos@hospentra.com")
                .phone("+51987654321")
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String username, String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority(role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void createReservationSuccessCalculatesTotalPrice() {
        authenticateAs("recep", "ROLE_RECEPTIONIST");
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(8);

        ReservationRequest request = new ReservationRequest(
                10L,
                101L,
                checkIn,
                checkOut,
                PaymentMethod.CREDIT_CARD,
                "4242"
        );

        when(guestRepository.findById(10L)).thenReturn(Optional.of(standardGuest));
        when(roomRepository.findByIdWithLock(101L)).thenReturn(Optional.of(standardRoom));
        when(reservationRepository.existsOverlappingReservation(eq(101L), eq(checkIn), eq(checkOut), anyList()))
                .thenReturn(false);

        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation r = invocation.getArgument(0);
            r.setId(500L);
            return r;
        });

        ReservationResponse response = reservationService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(500L);
        assertThat(response.totalPrice()).isEqualByComparingTo(new BigDecimal("900.00"));
        assertThat(response.status()).isEqualTo(ReservationStatus.PENDING);
        assertThat(response.room().roomNumber()).isEqualTo("101");
        verify(sseNotificationService, times(1)).broadcast(any(NotificationEvent.class));
    }

    @Test
    void createReservationFailsWhenRoomUnderMaintenance() {
        authenticateAs("recep", "ROLE_RECEPTIONIST");
        standardRoom.setStatus(RoomStatus.MAINTENANCE);
        LocalDate checkIn = LocalDate.now().plusDays(2);
        LocalDate checkOut = LocalDate.now().plusDays(4);

        ReservationRequest request = new ReservationRequest(
                10L, 101L, checkIn, checkOut, PaymentMethod.PAY_AT_CHECKIN, null
        );

        when(guestRepository.findById(10L)).thenReturn(Optional.of(standardGuest));
        when(roomRepository.findByIdWithLock(101L)).thenReturn(Optional.of(standardRoom));

        assertThatThrownBy(() -> reservationService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maintenance");
    }

    @Test
    void createReservationFailsWhenOverlappingReservationExists() {
        authenticateAs("recep", "ROLE_RECEPTIONIST");
        LocalDate checkIn = LocalDate.now().plusDays(2);
        LocalDate checkOut = LocalDate.now().plusDays(5);

        ReservationRequest request = new ReservationRequest(
                10L, 101L, checkIn, checkOut, PaymentMethod.PAY_AT_CHECKIN, null
        );

        when(guestRepository.findById(10L)).thenReturn(Optional.of(standardGuest));
        when(roomRepository.findByIdWithLock(101L)).thenReturn(Optional.of(standardRoom));
        when(reservationRepository.existsOverlappingReservation(eq(101L), eq(checkIn), eq(checkOut), anyList()))
                .thenReturn(true);

        assertThatThrownBy(() -> reservationService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not available for the selected dates");
    }

    @Test
    void createReservationFailsWhenOverlappingMaintenanceBlockExists() {
        authenticateAs("recep", "ROLE_RECEPTIONIST");
        LocalDate checkIn = LocalDate.now().plusDays(2);
        LocalDate checkOut = LocalDate.now().plusDays(5);

        ReservationRequest request = new ReservationRequest(
                10L, 101L, checkIn, checkOut, PaymentMethod.PAY_AT_CHECKIN, null
        );

        when(guestRepository.findById(10L)).thenReturn(Optional.of(standardGuest));
        when(roomRepository.findByIdWithLock(101L)).thenReturn(Optional.of(standardRoom));
        when(reservationRepository.existsOverlappingReservation(eq(101L), eq(checkIn), eq(checkOut), anyList()))
                .thenReturn(false);
        when(maintenanceBlockRepository.existsOverlappingActiveBlock(eq(101L), eq(checkIn), eq(checkOut)))
                .thenReturn(true);

        assertThatThrownBy(() -> reservationService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("under maintenance / out of order");
    }

    @Test
    void createReservation_WhenCustomerRequestsAnotherGuestId_ThrowsAccessDeniedException() {
        authenticateAs("carlos.m", "ROLE_CUSTOMER");
        User customerUser = User.builder().id(100L).username("carlos.m").role(Role.ROLE_CUSTOMER).guest(standardGuest).build();
        when(userRepository.findByUsername("carlos.m")).thenReturn(Optional.of(customerUser));

        ReservationRequest request = new ReservationRequest(
                999L, 101L, LocalDate.now().plusDays(2), LocalDate.now().plusDays(4), PaymentMethod.PAY_AT_CHECKIN, null
        );

        assertThatThrownBy(() -> reservationService.create(request))
                .isInstanceOf(AccessDeniedException.class);
        verify(guestRepository, never()).findById(999L);
    }

    @Test
    void createReservation_WhenCustomerRequestsNullGuestId_UsesOwnGuestAndSucceeds() {
        authenticateAs("carlos.m", "ROLE_CUSTOMER");
        User customerUser = User.builder().id(100L).username("carlos.m").role(Role.ROLE_CUSTOMER).guest(standardGuest).build();
        when(userRepository.findByUsername("carlos.m")).thenReturn(Optional.of(customerUser));
        when(roomRepository.findByIdWithLock(101L)).thenReturn(Optional.of(standardRoom));
        when(reservationRepository.existsOverlappingReservation(eq(101L), any(), any(), anyList())).thenReturn(false);
        when(maintenanceBlockRepository.existsOverlappingActiveBlock(eq(101L), any(), any())).thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> {
            Reservation r = i.getArgument(0);
            r.setId(501L);
            return r;
        });

        ReservationRequest request = new ReservationRequest(
                null, 101L, LocalDate.now().plusDays(2), LocalDate.now().plusDays(4), PaymentMethod.PAY_AT_CHECKIN, null
        );

        ReservationResponse response = reservationService.create(request);

        assertThat(response.id()).isEqualTo(501L);
        assertThat(response.guest().id()).isEqualTo(10L);
    }

    @Test
    void createReservation_WhenUnauthenticated_ThrowsAccessDeniedException() {
        SecurityContextHolder.clearContext();
        ReservationRequest request = new ReservationRequest(
                10L, 101L, LocalDate.now().plusDays(2), LocalDate.now().plusDays(4), PaymentMethod.PAY_AT_CHECKIN, null
        );

        assertThatThrownBy(() -> reservationService.create(request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void confirmReservationSuccess() {
        Reservation reservation = Reservation.builder()
                .id(1L)
                .guest(standardGuest)
                .room(standardRoom)
                .checkInDate(LocalDate.now().plusDays(3))
                .checkOutDate(LocalDate.now().plusDays(5))
                .status(ReservationStatus.PENDING)
                .totalPrice(new BigDecimal("600.00"))
                .build();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        ReservationResponse response = reservationService.confirm(1L);

        assertThat(response.status()).isEqualTo(ReservationStatus.CONFIRMED);
        verify(sseNotificationService, times(1)).broadcast(any(NotificationEvent.class));
    }

    @Test
    void confirmReservationFailsWhenNotPending() {
        Reservation reservation = Reservation.builder()
                .id(1L)
                .checkInDate(LocalDate.now().plusDays(3))
                .status(ReservationStatus.CONFIRMED)
                .build();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.confirm(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only PENDING");
    }

    @Test
    void checkInSuccessUpdatesRoomToOccupied() {
        Reservation reservation = Reservation.builder()
                .id(1L)
                .guest(standardGuest)
                .room(standardRoom)
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(2))
                .status(ReservationStatus.CONFIRMED)
                .paymentMethod(PaymentMethod.PAY_AT_CHECKIN)
                .paymentStatus(PaymentStatus.PENDING)
                .totalPrice(new BigDecimal("600.00"))
                .build();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        ReservationResponse response = reservationService.checkIn(1L);

        assertThat(response.status()).isEqualTo(ReservationStatus.CHECKED_IN);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(standardRoom.getStatus()).isEqualTo(RoomStatus.OCCUPIED);
        verify(roomRepository, times(1)).save(standardRoom);
        verify(sseNotificationService, times(2)).broadcast(any(NotificationEvent.class));
    }

    @Test
    void cancelReservationFullRefundWhenMoreThan48Hours() {
        Reservation reservation = Reservation.builder()
                .id(1L)
                .guest(standardGuest)
                .room(standardRoom)
                .checkInDate(LocalDate.now().plusDays(5))
                .checkOutDate(LocalDate.now().plusDays(7))
                .status(ReservationStatus.CONFIRMED)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .paymentStatus(PaymentStatus.PAID)
                .totalPrice(new BigDecimal("600.00"))
                .build();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        ReservationResponse response = reservationService.cancel(1L);

        assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(sseNotificationService, times(1)).broadcast(any(NotificationEvent.class));
    }

    @Test
    void cancelReservationPartialRefundWhenLessThan48Hours() {
        Reservation reservation = Reservation.builder()
                .id(1L)
                .guest(standardGuest)
                .room(standardRoom)
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(2))
                .status(ReservationStatus.CONFIRMED)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .paymentStatus(PaymentStatus.PAID)
                .totalPrice(new BigDecimal("600.00"))
                .build();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        ReservationResponse response = reservationService.cancel(1L);

        assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
    }

    @Test
    void cancelReservationFreesRoomWhenAlreadyCheckedIn() {
        standardRoom.setStatus(RoomStatus.OCCUPIED);
        Reservation reservation = Reservation.builder()
                .id(1L)
                .guest(standardGuest)
                .room(standardRoom)
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(2))
                .status(ReservationStatus.CHECKED_IN)
                .paymentMethod(PaymentMethod.PAY_AT_CHECKIN)
                .totalPrice(new BigDecimal("600.00"))
                .build();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        ReservationResponse response = reservationService.cancel(1L);

        assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(standardRoom.getStatus()).isEqualTo(RoomStatus.AVAILABLE);
        verify(roomRepository, times(1)).save(standardRoom);
    }

    @Test
    void completeReservationSuccessUpdatesRoomToMaintenance() {
        standardRoom.setStatus(RoomStatus.OCCUPIED);
        Reservation reservation = Reservation.builder()
                .id(1L)
                .guest(standardGuest)
                .room(standardRoom)
                .checkInDate(LocalDate.now().minusDays(2))
                .checkOutDate(LocalDate.now())
                .status(ReservationStatus.CHECKED_IN)
                .totalPrice(new BigDecimal("600.00"))
                .build();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(roomOrderRepository.findByReservationIdOrderByOrderedAtDesc(1L)).thenReturn(Collections.emptyList());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        ReservationResponse response = reservationService.complete(1L);

        assertThat(response.status()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(standardRoom.getStatus()).isEqualTo(RoomStatus.MAINTENANCE);
        verify(roomRepository, times(1)).save(standardRoom);
        verify(sseNotificationService, times(2)).broadcast(any(NotificationEvent.class));
    }

    @Test
    void completeReservationWithSettlementDetails_marksOrdersPaidAndSetsSettlementFields() {
        standardRoom.setStatus(RoomStatus.OCCUPIED);
        Reservation reservation = Reservation.builder()
                .id(1L)
                .guest(standardGuest)
                .room(standardRoom)
                .checkInDate(LocalDate.now().minusDays(2))
                .checkOutDate(LocalDate.now())
                .status(ReservationStatus.CHECKED_IN)
                .totalPrice(new BigDecimal("600.00"))
                .build();

        RoomOrder order1 = RoomOrder.builder()
                .id(201L)
                .reservationId(1L)
                .roomNumber("101")
                .guestName("Carlos Mendoza")
                .totalAmount(new BigDecimal("84.00"))
                .status(OrderStatus.DELIVERED)
                .paid(false)
                .build();

        CheckOutSettlementRequest settlementRequest = new CheckOutSettlementRequest(
                "POS_TERMINAL",
                new BigDecimal("84.00"),
                "AUTH-POS-1234",
                "Cobro verificado con POS"
        );

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(roomOrderRepository.findByReservationIdOrderByOrderedAtDesc(1L)).thenReturn(List.of(order1));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        ReservationResponse response = reservationService.complete(1L, settlementRequest);

        assertThat(response.status()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(response.settlementMethod()).isEqualTo("POS_TERMINAL");
        assertThat(response.settlementAmount()).isEqualByComparingTo(new BigDecimal("84.00"));
        assertThat(response.authCode()).isEqualTo("AUTH-POS-1234");
        assertThat(order1.isPaid()).isTrue();
        verify(roomOrderRepository, times(1)).saveAll(anyList());
    }

    @Test
    void expressCheckOut_whenValidGuest_marksOrdersPaidAndSetsExpressSettlement() {
        standardRoom.setStatus(RoomStatus.OCCUPIED);
        Reservation reservation = Reservation.builder()
                .id(1L)
                .guest(standardGuest)
                .room(standardRoom)
                .checkInDate(LocalDate.now().minusDays(3))
                .checkOutDate(LocalDate.now())
                .status(ReservationStatus.CHECKED_IN)
                .totalPrice(new BigDecimal("900.00"))
                .build();

        RoomOrder order = RoomOrder.builder()
                .id(301L)
                .reservationId(1L)
                .roomNumber("101")
                .guestName("Carlos Mendoza")
                .totalAmount(new BigDecimal("150.00"))
                .status(OrderStatus.DELIVERED)
                .paid(false)
                .build();

        User user = User.builder()
                .id(100L)
                .username("carlos.m")
                .role(Role.ROLE_CUSTOMER)
                .guest(standardGuest)
                .build();

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("carlos.m");
        SecurityContext secCtx = mock(SecurityContext.class);
        when(secCtx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(secCtx);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(userRepository.findByUsername("carlos.m")).thenReturn(Optional.of(user));
        when(roomOrderRepository.findByReservationIdOrderByOrderedAtDesc(1L)).thenReturn(List.of(order));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        ExpressCheckOutRequest request = new ExpressCheckOutRequest(
                new BigDecimal("150.00"),
                "4242",
                "pi_express_123",
                "Check-Out Express con tarjeta registrada"
        );

        ReservationResponse response = reservationService.expressCheckOut(1L, request);

        assertThat(response.status()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(response.settlementMethod()).isEqualTo("ONLINE_EXPRESS");
        assertThat(response.settlementAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(response.cardLast4()).isEqualTo("4242");
        assertThat(response.paymentIntentId()).isEqualTo("pi_express_123");
        assertThat(order.isPaid()).isTrue();
        assertThat(standardRoom.getStatus()).isEqualTo(RoomStatus.MAINTENANCE);
        verify(roomOrderRepository, times(1)).saveAll(anyList());
        verify(auditLogService, times(1)).logAction(eq("EXPRESS_CHECK_OUT"), eq("Reservation"), eq("HOSP-1-2026"), any(), eq("SUCCESS"));

        SecurityContextHolder.clearContext();
    }

    @Test
    void expressCheckOut_whenDifferentGuest_throwsAccessDeniedException() {
        Reservation reservation = Reservation.builder()
                .id(1L)
                .guest(standardGuest)
                .room(standardRoom)
                .status(ReservationStatus.CHECKED_IN)
                .build();

        Guest otherGuest = Guest.builder().id(999L).firstName("Intruso").build();
        User otherUser = User.builder().id(200L).username("intruso").guest(otherGuest).build();

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("intruso");
        SecurityContext secCtx = mock(SecurityContext.class);
        when(secCtx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(secCtx);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(userRepository.findByUsername("intruso")).thenReturn(Optional.of(otherUser));

        ExpressCheckOutRequest request = new ExpressCheckOutRequest(
                BigDecimal.ZERO, "4242", null, null
        );

        assertThatThrownBy(() -> reservationService.expressCheckOut(1L, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No tiene permisos para realizar check-out express");

        SecurityContextHolder.clearContext();
    }

    @Test
    void expressCheckOut_whenNotCheckedIn_throwsIllegalArgumentException() {
        Reservation reservation = Reservation.builder()
                .id(1L)
                .guest(standardGuest)
                .room(standardRoom)
                .status(ReservationStatus.CONFIRMED)
                .build();

        User user = User.builder().id(100L).username("carlos.m").guest(standardGuest).build();

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("carlos.m");
        SecurityContext secCtx = mock(SecurityContext.class);
        when(secCtx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(secCtx);

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(userRepository.findByUsername("carlos.m")).thenReturn(Optional.of(user));

        ExpressCheckOutRequest request = new ExpressCheckOutRequest(
                BigDecimal.ZERO, "4242", null, null
        );

        assertThatThrownBy(() -> reservationService.expressCheckOut(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Solo es posible realizar Check-Out Express en reservaciones activas");

        SecurityContextHolder.clearContext();
    }

    @Test
    void submitPreCheckIn_whenValid_shouldCompletePreCheckInAndEmitEvents() {
        Reservation reservation = Reservation.builder()
                .id(1L)
                .guest(standardGuest)
                .room(standardRoom)
                .checkInDate(LocalDate.now().plusDays(2))
                .checkOutDate(LocalDate.now().plusDays(5))
                .status(ReservationStatus.CONFIRMED)
                .totalPrice(new BigDecimal("900.00"))
                .preCheckInCompleted(false)
                .build();

        com.hotel.web.dto.PreCheckInRequest request = new com.hotel.web.dto.PreCheckInRequest(
                "PASSPORT",
                "AB1234567",
                "Española",
                "15:30",
                "Piso alto con vista al mar"
        );

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        ReservationResponse response = reservationService.submitPreCheckIn(1L, request);

        assertThat(response.preCheckInCompleted()).isTrue();
        assertThat(response.documentType()).isEqualTo("PASSPORT");
        assertThat(response.documentNumber()).isEqualTo("AB1234567");
        assertThat(response.nationality()).isEqualTo("Española");
        assertThat(response.estimatedArrivalTime()).isEqualTo("15:30");
        assertThat(response.specialRequests()).isEqualTo("Piso alto con vista al mar");

        verify(sseNotificationService, times(1)).broadcast(any(NotificationEvent.class));
        verify(auditLogService, times(1)).logAction(eq("PRE_CHECK_IN"), eq("Reservation"), eq("HOSP-1-2026"), any(), eq("SUCCESS"));
    }

    @Test
    void submitPreCheckIn_whenCancelledReservation_shouldThrowIllegalArgumentException() {
        Reservation reservation = Reservation.builder()
                .id(1L)
                .guest(standardGuest)
                .room(standardRoom)
                .status(ReservationStatus.CANCELLED)
                .build();

        com.hotel.web.dto.PreCheckInRequest request = new com.hotel.web.dto.PreCheckInRequest(
                "PASSPORT", "AB1234567", "Española", "15:30", null
        );

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.submitPreCheckIn(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No es posible realizar pre-check-in en una reservación cancelada o completada");
    }
}
