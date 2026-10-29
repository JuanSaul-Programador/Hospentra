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
import com.hotel.domain.repository.ReservationRepository;
import com.hotel.domain.repository.RoomRepository;
import com.hotel.domain.repository.UserRepository;
import com.hotel.web.dto.ReservationRequest;
import com.hotel.web.dto.ReservationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
                reservationMapper,
                sseNotificationService,
                emailService
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

    @Test
    void createReservationSuccessCalculatesTotalPrice() {
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
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        ReservationResponse response = reservationService.complete(1L);

        assertThat(response.status()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(standardRoom.getStatus()).isEqualTo(RoomStatus.MAINTENANCE);
        verify(roomRepository, times(1)).save(standardRoom);
        verify(sseNotificationService, times(2)).broadcast(any(NotificationEvent.class));
    }
}
