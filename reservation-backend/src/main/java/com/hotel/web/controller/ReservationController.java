package com.hotel.web.controller;

import com.hotel.application.services.ReservationService;
import com.hotel.domain.model.ReservationStatus;
import com.hotel.web.dto.ReservationRequest;
import com.hotel.web.dto.ReservationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping("/my")
    public ResponseEntity<List<ReservationResponse>> findMyReservations() {
        return ResponseEntity.ok(reservationService.findMyReservations());
    }

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> findAll(@RequestParam(required = false) ReservationStatus status) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isStaff = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_RECEPTIONIST"));
        if (!isStaff) {
            return ResponseEntity.ok(reservationService.findMyReservations());
        }

        if (status != null) {
            return ResponseEntity.ok(reservationService.findByStatus(status));
        }
        return ResponseEntity.ok(reservationService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.findById(id));
    }

    @GetMapping("/guest/{guestId}")
    public ResponseEntity<List<ReservationResponse>> findByGuestId(@PathVariable Long guestId) {
        return ResponseEntity.ok(reservationService.findByGuestId(guestId));
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.create(request));
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<ReservationResponse> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.confirm(id));
    }

    @PatchMapping("/{id}/check-in")
    public ResponseEntity<ReservationResponse> checkIn(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.checkIn(id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ReservationResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.cancel(id));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ReservationResponse> complete(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) com.hotel.web.dto.CheckOutSettlementRequest settlementRequest) {
        return ResponseEntity.ok(reservationService.complete(id, settlementRequest));
    }

    @PostMapping("/{id}/express-checkout")
    public ResponseEntity<ReservationResponse> expressCheckOut(
            @PathVariable Long id,
            @Valid @RequestBody com.hotel.web.dto.ExpressCheckOutRequest request) {
        return ResponseEntity.ok(reservationService.expressCheckOut(id, request));
    }

    @PatchMapping("/{id}/pre-check-in")
    public ResponseEntity<ReservationResponse> submitPreCheckIn(
            @PathVariable Long id,
            @Valid @RequestBody com.hotel.web.dto.PreCheckInRequest request) {
        return ResponseEntity.ok(reservationService.submitPreCheckIn(id, request));
    }
}
