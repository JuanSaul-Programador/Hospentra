package com.hotel.application.mapper;

import com.hotel.domain.model.Guest;
import com.hotel.domain.model.PaymentMethod;
import com.hotel.domain.model.PaymentStatus;
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
                reservation.getCreatedAt(),
                reservation.getPaymentMethod(),
                reservation.getPaymentStatus(),
                reservation.getCardLast4(),
                reservation.getPaymentIntentId(),
                reservation.getAuthCode(),
                reservation.isPreCheckInCompleted(),
                reservation.getPreCheckInAt(),
                reservation.getDocumentType(),
                reservation.getDocumentNumber(),
                reservation.getNationality(),
                reservation.getEstimatedArrivalTime(),
                reservation.getSpecialRequests()
        );
    }

    public Reservation toEntity(ReservationRequest request, Guest guest, Room room, BigDecimal totalPrice) {
        PaymentMethod method = request.paymentMethod() != null ? request.paymentMethod() : PaymentMethod.PAY_AT_CHECKIN;
        PaymentStatus status = method == PaymentMethod.CREDIT_CARD ? PaymentStatus.PAID : PaymentStatus.PENDING;

        return Reservation.builder()
                .guest(guest)
                .room(room)
                .checkInDate(request.checkInDate())
                .checkOutDate(request.checkOutDate())
                .status(ReservationStatus.PENDING)
                .totalPrice(totalPrice)
                .paymentMethod(method)
                .paymentStatus(status)
                .cardLast4(request.cardLast4())
                .paymentIntentId(request.paymentIntentId())
                .authCode(request.authCode())
                .build();
    }
}
