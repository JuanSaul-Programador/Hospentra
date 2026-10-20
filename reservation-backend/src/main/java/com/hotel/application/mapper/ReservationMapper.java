package com.hotel.application.mapper;

import com.hotel.domain.model.Guest;
import com.hotel.domain.model.Reservation;
import com.hotel.domain.model.ReservationStatus;
import com.hotel.domain.model.Room;
import com.hotel.web.dto.ReservationRequest;
import com.hotel.web.dto.ReservationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ReservationMapper {

    private final GuestMapper guestMapper;
    private final RoomMapper roomMapper;

    public ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                guestMapper.toResponse(reservation.getGuest()),
                roomMapper.toResponse(reservation.getRoom()),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getStatus(),
                reservation.getTotalPrice(),
                reservation.getCreatedAt()
        );
    }

    public Reservation toEntity(ReservationRequest request, Guest guest, Room room, BigDecimal totalPrice) {
        return Reservation.builder()
                .guest(guest)
                .room(room)
                .checkInDate(request.checkInDate())
                .checkOutDate(request.checkOutDate())
                .status(ReservationStatus.PENDING)
                .totalPrice(totalPrice)
                .build();
    }
}
