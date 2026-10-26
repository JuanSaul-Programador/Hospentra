package com.hotel.web.controller;

import com.hotel.application.services.RoomOrderService;
import com.hotel.web.dto.CreateRoomOrderRequest;
import com.hotel.web.dto.RoomOrderResponse;
import com.hotel.web.dto.UpdateOrderStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/room-orders")
@RequiredArgsConstructor
public class RoomOrderController {

    private final RoomOrderService roomOrderService;

    @PostMapping
    public ResponseEntity<RoomOrderResponse> createOrder(@Valid @RequestBody CreateRoomOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomOrderService.createOrder(request));
    }

    @GetMapping("/active")
    public ResponseEntity<List<RoomOrderResponse>> findActiveKitchenOrders() {
        return ResponseEntity.ok(roomOrderService.findActiveKitchenOrders());
    }

    @GetMapping
    public ResponseEntity<List<RoomOrderResponse>> findAll() {
        return ResponseEntity.ok(roomOrderService.findAllOrders());
    }

    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<List<RoomOrderResponse>> findByReservationId(@PathVariable Long reservationId) {
        return ResponseEntity.ok(roomOrderService.findByReservationId(reservationId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RoomOrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return ResponseEntity.ok(roomOrderService.updateOrderStatus(id, request.status()));
    }

    @PatchMapping("/{id}/assign-therapist")
    public ResponseEntity<RoomOrderResponse> assignTherapist(
            @PathVariable Long id,
            @Valid @RequestBody com.hotel.web.dto.AssignTherapistRequest request
    ) {
        return ResponseEntity.ok(roomOrderService.assignTherapist(id, request.therapistId()));
    }
}
