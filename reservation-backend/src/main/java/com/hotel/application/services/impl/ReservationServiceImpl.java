package com.hotel.application.services.impl;

import com.hotel.application.mapper.ReservationMapper;
import com.hotel.application.services.ReservationService;
import com.hotel.domain.exception.ResourceNotFoundException;
import com.hotel.domain.model.*;
import com.hotel.domain.repository.GuestRepository;
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
    private final ReservationMapper reservationMapper;

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

        long nights = ChronoUnit.DAYS.between(request.checkInDate(), request.checkOutDate());
        BigDecimal totalPrice = room.getRoomType().getPricePerNight().multiply(BigDecimal.valueOf(nights));

        Reservation reservation = reservationMapper.toEntity(request, guest, room, totalPrice);
        return reservationMapper.toResponse(reservationRepository.save(reservation));
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
        return reservationMapper.toResponse(reservationRepository.save(reservation));
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

        return reservationMapper.toResponse(reservationRepository.save(reservation));
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
        return reservationMapper.toResponse(reservationRepository.save(reservation));
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

        return reservationMapper.toResponse(reservationRepository.save(reservation));
    }
}
