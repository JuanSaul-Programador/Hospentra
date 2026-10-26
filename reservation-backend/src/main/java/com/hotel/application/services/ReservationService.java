package com.hotel.application.services;

import com.hotel.domain.model.ReservationStatus;
import com.hotel.web.dto.ReservationRequest;
import com.hotel.web.dto.ReservationResponse;

import java.util.List;

public interface ReservationService {

    List<ReservationResponse> findAll();

    List<ReservationResponse> findMyReservations();

    List<ReservationResponse> findByStatus(ReservationStatus status);

    List<ReservationResponse> findByGuestId(Long guestId);

    ReservationResponse findById(Long id);

    ReservationResponse create(ReservationRequest request);

    ReservationResponse confirm(Long id);

    ReservationResponse checkIn(Long id);

    ReservationResponse cancel(Long id);

    ReservationResponse complete(Long id);
}
