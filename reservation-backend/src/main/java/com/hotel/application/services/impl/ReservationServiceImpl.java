package com.hotel.application.services.impl;

import com.hotel.application.dto.notification.NotificationEvent;
import com.hotel.application.mapper.ReservationMapper;
import com.hotel.application.services.AuditLogService;
import com.hotel.application.services.EmailService;
import com.hotel.application.services.ReservationService;
import com.hotel.application.services.SseNotificationService;
import com.hotel.domain.exception.ResourceNotFoundException;
import com.hotel.domain.model.*;
import com.hotel.domain.repository.GuestRepository;
import com.hotel.domain.repository.MaintenanceBlockRepository;
import com.hotel.domain.repository.ReservationRepository;
import com.hotel.domain.repository.RoomRepository;
import com.hotel.domain.repository.UserRepository;
import com.hotel.web.dto.ReservationRequest;
import com.hotel.web.dto.ReservationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final GuestRepository guestRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final MaintenanceBlockRepository maintenanceBlockRepository;
    private final ReservationMapper reservationMapper;
    private final SseNotificationService sseNotificationService;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    @Override
    public List<ReservationResponse> findAll() {
        return reservationRepository.findAll().stream()
                .map(reservationMapper::toResponse)
                .toList();
    }

    @Override
    public List<ReservationResponse> findMyReservations() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return List.of();
        }
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getGuest() == null) {
            return List.of();
        }
        return reservationRepository.findByGuestId(user.getGuest().getId()).stream()
                .map(reservationMapper::toResponse)
                .toList();
    }

    @Override
    public List<ReservationResponse> findByStatus(ReservationStatus status) {
        return reservationRepository.findByStatus(status).stream()
                .map(reservationMapper::toResponse)
                .toList();
    }

    @Override
    public List<ReservationResponse> findByGuestId(Long guestId) {
        if (!guestRepository.existsById(guestId)) {
            throw new ResourceNotFoundException("Guest not found with id: " + guestId);
        }
        return reservationRepository.findByGuestId(guestId).stream()
                .map(reservationMapper::toResponse)
                .toList();
    }

    @Override
    public ReservationResponse findById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isStaff = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_RECEPTIONIST"));
            if (!isStaff) {
                String username = authentication.getName();
                User user = userRepository.findByUsername(username).orElse(null);
                if (user == null || user.getGuest() == null || !reservation.getGuest().getId().equals(user.getGuest().getId())) {
                    throw new AccessDeniedException("No tiene permisos para acceder a esta reservación");
                }
            }
        }

        return reservationMapper.toResponse(reservation);
    }

    @Override
    @Transactional
    public ReservationResponse create(ReservationRequest request) {
        if (!request.checkOutDate().isAfter(request.checkInDate())) {
            throw new IllegalArgumentException("Check-out date must be after check-in date");
        }

        Guest guest = resolveGuest(request.guestId());

        Room room = roomRepository.findByIdWithLock(request.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + request.roomId()));

        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new IllegalArgumentException("Room " + room.getRoomNumber() + " is currently under maintenance");
        }

        List<ReservationStatus> activeStatuses = List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN);
        boolean hasOverlap = reservationRepository.existsOverlappingReservation(
                room.getId(), request.checkInDate(), request.checkOutDate(), activeStatuses);

        if (hasOverlap) {
            throw new IllegalArgumentException("Room " + room.getRoomNumber() + " is not available for the selected dates");
        }

        boolean isBlocked = maintenanceBlockRepository.existsOverlappingActiveBlock(
                room.getId(), request.checkInDate(), request.checkOutDate());

        if (isBlocked) {
            throw new IllegalArgumentException("Room " + room.getRoomNumber() + " is currently under maintenance / out of order for the selected dates");
        }

        long nights = ChronoUnit.DAYS.between(request.checkInDate(), request.checkOutDate());
        BigDecimal totalPrice = room.getRoomType().getPricePerNight().multiply(BigDecimal.valueOf(nights));

        Reservation reservation = reservationMapper.toEntity(request, guest, room, totalPrice);
        if (reservation.getPaymentMethod() == PaymentMethod.CREDIT_CARD && (reservation.getAuthCode() == null || reservation.getAuthCode().isBlank())) {
            reservation.setAuthCode("AUTH-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        Reservation saved = reservationRepository.save(reservation);
        ReservationResponse response = reservationMapper.toResponse(saved);

        NotificationEvent event = NotificationEvent.of(
                "RESERVATION_CREATED",
                "FRONT_DESK",
                saved.getId(),
                "Nueva Reservación #HOSP-" + saved.getId(),
                guest.getFirstName() + " " + guest.getLastName() + " reservó Suite " + room.getRoomNumber(),
                response
        );
        sseNotificationService.broadcast(event);

        emailService.sendBookingConfirmation(
                guest.getEmail(),
                guest.getFirstName(),
                room.getRoomType().getName(),
                saved.getCheckInDate().toString(),
                saved.getCheckOutDate().toString(),
                totalPrice.doubleValue(),
                saved.getAuthCode() != null ? saved.getAuthCode() : ("AUTH-" + saved.getId())
        );

        auditLogService.logAction(
                "CREATE_RESERVATION",
                "Reservation",
                "HOSP-" + saved.getId() + "-2026",
                "Reserva creada para " + guest.getFirstName() + " " + guest.getLastName() + " en Suite " + room.getRoomNumber() + " (" + nights + " noches)",
                "SUCCESS"
        );

        return response;
    }

    private Guest resolveGuest(Long requestedGuestId) {
        if (requestedGuestId != null) {
            return guestRepository.findById(requestedGuestId)
                    .orElseThrow(() -> new ResourceNotFoundException("Guest not found with id: " + requestedGuestId));
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
            if (user.getGuest() != null) {
                return user.getGuest();
            }
        }

        throw new IllegalArgumentException("A valid guestId must be provided or your user account must have a linked guest profile");
    }

    @Override
    @Transactional
    public ReservationResponse confirm(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING reservations can be confirmed");
        }

        if (reservation.getCheckInDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot confirm a reservation whose check-in date has already passed");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        Reservation saved = reservationRepository.save(reservation);
        ReservationResponse response = reservationMapper.toResponse(saved);

        NotificationEvent event = NotificationEvent.of(
                "RESERVATION_STATUS_CHANGED",
                "FRONT_DESK",
                saved.getId(),
                "Reservación Confirmada #HOSP-" + saved.getId(),
                "Reserva de Suite " + saved.getRoom().getRoomNumber() + " confirmada",
                response
        );
        sseNotificationService.broadcast(event);

        auditLogService.logAction(
                "CONFIRM_RESERVATION",
                "Reservation",
                "HOSP-" + saved.getId() + "-2026",
                "Reserva confirmada en Suite " + saved.getRoom().getRoomNumber(),
                "SUCCESS"
        );

        return response;
    }

    @Override
    @Transactional
    public ReservationResponse checkIn(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED && reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING or CONFIRMED reservations can be checked in");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        if (reservation.getPaymentMethod() == PaymentMethod.PAY_AT_CHECKIN) {
            reservation.setPaymentStatus(PaymentStatus.PAID);
        }

        Room room = reservation.getRoom();
        room.setStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);

        Reservation saved = reservationRepository.save(reservation);
        ReservationResponse response = reservationMapper.toResponse(saved);

        NotificationEvent resEvent = NotificationEvent.of(
                "RESERVATION_STATUS_CHANGED",
                "FRONT_DESK",
                saved.getId(),
                "Check-In Registrado #HOSP-" + saved.getId(),
                saved.getGuest().getFirstName() + " " + saved.getGuest().getLastName() + " ingresó a Suite " + room.getRoomNumber(),
                response
        );
        sseNotificationService.broadcast(resEvent);

        NotificationEvent roomEvent = NotificationEvent.of(
                "ROOM_STATUS_CHANGED",
                "ROOM_RACK",
                room.getId(),
                "Suite " + room.getRoomNumber() + " Ocupada",
                "Huésped en casa",
                room
        );
        sseNotificationService.broadcast(roomEvent);

        auditLogService.logAction(
                "CHECK_IN",
                "Reservation",
                "HOSP-" + saved.getId() + "-2026",
                "Check-in completado para Suite " + room.getRoomNumber() + " por huésped " + saved.getGuest().getFirstName() + " " + saved.getGuest().getLastName(),
                "SUCCESS"
        );

        return response;
    }

    @Override
    @Transactional
    public ReservationResponse cancel(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isStaff = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_RECEPTIONIST"));
            if (!isStaff) {
                String username = authentication.getName();
                User user = userRepository.findByUsername(username).orElse(null);
                if (user == null || user.getGuest() == null || !reservation.getGuest().getId().equals(user.getGuest().getId())) {
                    throw new AccessDeniedException("No tiene permisos para cancelar esta reservación");
                }
            }
        }

        if (reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new IllegalArgumentException("A completed reservation cannot be cancelled");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalArgumentException("This reservation is already cancelled");
        }

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN) {
            Room room = reservation.getRoom();
            room.setStatus(RoomStatus.AVAILABLE);
            roomRepository.save(room);
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        if (reservation.getPaymentMethod() == PaymentMethod.CREDIT_CARD) {
            long hoursUntilCheckIn = ChronoUnit.HOURS.between(LocalDateTime.now(), reservation.getCheckInDate().atTime(15, 0));
            if (hoursUntilCheckIn >= 48) {
                reservation.setPaymentStatus(PaymentStatus.REFUNDED);
            } else {
                reservation.setPaymentStatus(PaymentStatus.PARTIALLY_REFUNDED);
            }
        }
        Reservation saved = reservationRepository.save(reservation);
        ReservationResponse response = reservationMapper.toResponse(saved);

        NotificationEvent event = NotificationEvent.of(
                "RESERVATION_STATUS_CHANGED",
                "FRONT_DESK",
                saved.getId(),
                "Reservación Cancelada #HOSP-" + saved.getId(),
                "Reserva de Suite " + saved.getRoom().getRoomNumber() + " cancelada",
                response
        );
        sseNotificationService.broadcast(event);

        auditLogService.logAction(
                "CANCEL_RESERVATION",
                "Reservation",
                "HOSP-" + saved.getId() + "-2026",
                "Reserva cancelada para Suite " + saved.getRoom().getRoomNumber(),
                "SUCCESS"
        );

        return response;
    }

    @Override
    @Transactional
    public ReservationResponse complete(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("Only CHECKED_IN or CONFIRMED reservations can be completed");
        }

        reservation.setStatus(ReservationStatus.COMPLETED);

        Room room = reservation.getRoom();
        room.setStatus(RoomStatus.MAINTENANCE);
        roomRepository.save(room);

        Reservation saved = reservationRepository.save(reservation);
        ReservationResponse response = reservationMapper.toResponse(saved);

        NotificationEvent resEvent = NotificationEvent.of(
                "RESERVATION_STATUS_CHANGED",
                "FRONT_DESK",
                saved.getId(),
                "Check-Out Registrado #HOSP-" + saved.getId(),
                "Suite " + room.getRoomNumber() + " desocupada",
                response
        );
        sseNotificationService.broadcast(resEvent);

        NotificationEvent roomEvent = NotificationEvent.of(
                "ROOM_STATUS_CHANGED",
                "ROOM_RACK",
                room.getId(),
                "Suite " + room.getRoomNumber() + " en Limpieza",
                "Enviada a housekeeping",
                room
        );
        sseNotificationService.broadcast(roomEvent);

        auditLogService.logAction(
                "CHECK_OUT",
                "Reservation",
                "HOSP-" + saved.getId() + "-2026",
                "Check-out completado para Suite " + room.getRoomNumber() + ". Suite enviada a limpieza.",
                "SUCCESS"
        );

        return response;
    }
}
