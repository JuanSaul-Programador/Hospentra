package com.hotel.application.services.impl;

import com.hotel.application.services.RoomOrderService;
import com.hotel.domain.exception.ResourceNotFoundException;
import com.hotel.domain.model.*;
import com.hotel.domain.repository.ExtraServiceRepository;
import com.hotel.domain.repository.ReservationRepository;
import com.hotel.domain.repository.RoomOrderRepository;
import com.hotel.web.dto.CreateOrderItemRequest;
import com.hotel.web.dto.CreateRoomOrderRequest;
import com.hotel.web.dto.RoomOrderItemResponse;
import com.hotel.web.dto.RoomOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomOrderServiceImpl implements RoomOrderService {

    private final RoomOrderRepository roomOrderRepository;
    private final ReservationRepository reservationRepository;
    private final ExtraServiceRepository extraServiceRepository;

    @Override
    @Transactional
    public RoomOrderResponse createOrder(CreateRoomOrderRequest request) {
        Reservation reservation = reservationRepository.findById(request.reservationId())
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + request.reservationId()));

        String roomNumber = reservation.getRoom() != null ? reservation.getRoom().getRoomNumber() : "General";
        String guestName = reservation.getGuest() != null
                ? reservation.getGuest().getFirstName() + " " + reservation.getGuest().getLastName()
                : "Huésped";

        RoomOrder order = RoomOrder.builder()
                .reservationId(reservation.getId())
                .roomNumber(roomNumber)
                .guestName(guestName)
                .status(OrderStatus.RECEIVED)
                .notes(request.notes())
                .orderedAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (CreateOrderItemRequest itemReq : request.items()) {
            ExtraService extraService = extraServiceRepository.findById(itemReq.extraServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Extra service not found with id: " + itemReq.extraServiceId()));

            BigDecimal subtotal = extraService.getPrice().multiply(BigDecimal.valueOf(itemReq.quantity()));
            total = total.add(subtotal);

            RoomOrderItem orderItem = RoomOrderItem.builder()
                    .roomOrder(order)
                    .extraService(extraService)
                    .serviceName(extraService.getName())
                    .unitPrice(extraService.getPrice())
                    .quantity(itemReq.quantity())
                    .subtotal(subtotal)
                    .build();

            order.getItems().add(orderItem);
        }

        order.setTotalAmount(total);
        RoomOrder savedOrder = roomOrderRepository.save(order);

        return mapToResponse(savedOrder);
    }

    @Override
    public List<RoomOrderResponse> findByReservationId(Long reservationId) {
        return roomOrderRepository.findByReservationIdOrderByOrderedAtDesc(reservationId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<RoomOrderResponse> findActiveKitchenOrders() {
        return roomOrderRepository.findByStatusInOrderByOrderedAtAsc(
                List.of(OrderStatus.RECEIVED, OrderStatus.PREPARING, OrderStatus.READY_FOR_DELIVERY)
        ).stream().map(this::mapToResponse).toList();
    }

    @Override
    public List<RoomOrderResponse> findAllOrders() {
        return roomOrderRepository.findAllByOrderByOrderedAtDesc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public RoomOrderResponse updateOrderStatus(Long orderId, OrderStatus status) {
        RoomOrder order = roomOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Room order not found with id: " + orderId));

        order.setStatus(status);
        if (status == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
        }

        RoomOrder updated = roomOrderRepository.save(order);
        return mapToResponse(updated);
    }

    private RoomOrderResponse mapToResponse(RoomOrder order) {
        List<RoomOrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new RoomOrderItemResponse(
                        item.getId(),
                        item.getExtraService() != null ? item.getExtraService().getId() : null,
                        item.getServiceName(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getSubtotal()
                ))
                .toList();

        return new RoomOrderResponse(
                order.getId(),
                order.getReservationId(),
                order.getRoomNumber(),
                order.getGuestName(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getNotes(),
                order.getOrderedAt(),
                order.getDeliveredAt(),
                itemResponses
        );
    }
}
