package com.hotel.application.services.impl;

import com.hotel.application.dto.notification.NotificationEvent;
import com.hotel.application.services.RoomOrderService;
import com.hotel.application.services.SseNotificationService;
import com.hotel.domain.exception.ResourceNotFoundException;
import com.hotel.domain.model.*;
import com.hotel.domain.repository.ExtraServiceRepository;
import com.hotel.domain.repository.ReservationRepository;
import com.hotel.domain.repository.RoomOrderRepository;
import com.hotel.domain.repository.UserRepository;
import com.hotel.web.dto.CreateOrderItemRequest;
import com.hotel.web.dto.CreateRoomOrderRequest;
import com.hotel.web.dto.RoomOrderItemResponse;
import com.hotel.web.dto.RoomOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final UserRepository userRepository;
    private final com.hotel.domain.repository.TherapistRepository therapistRepository;
    private final SseNotificationService sseNotificationService;

    @Override
    @Transactional
    public RoomOrderResponse createOrder(CreateRoomOrderRequest request) {
        Reservation reservation = reservationRepository.findById(request.reservationId())
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + request.reservationId()));

        validateReservationAccess(reservation, "ordenar");

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
                .serviceLocation(request.serviceLocation() != null ? request.serviceLocation() : "IN_SUITE")
                .scheduledTime(request.scheduledTime())
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
        RoomOrderResponse response = mapToResponse(savedOrder);

        boolean hasKitchenItems = savedOrder.getItems().stream().anyMatch(item ->
                item.getExtraService() != null &&
                (item.getExtraService().getCategory() == ExtraCategory.GASTRONOMY ||
                 item.getExtraService().getCategory() == ExtraCategory.BAR_WINE));

        String channel = hasKitchenItems ? "KITCHEN" : "SPA";
        String categoryLabel = hasKitchenItems ? "Gastronomía / Bar" : "Bienestar & Spa";

        NotificationEvent event = NotificationEvent.of(
                "ORDER_CREATED",
                channel,
                savedOrder.getId(),
                "Nueva Comanda #" + savedOrder.getId(),
                "Suite " + savedOrder.getRoomNumber() + " solicitó " + categoryLabel,
                response
        );
        sseNotificationService.broadcast(event);

        return response;
    }

    @Override
    public List<RoomOrderResponse> findByReservationId(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + reservationId));
        validateReservationAccess(reservation, "ver comandas");

        return roomOrderRepository.findByReservationIdOrderByOrderedAtDesc(reservationId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void validateReservationAccess(Reservation reservation, String action) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isStaff = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_RECEPTIONIST") || a.getAuthority().equals("ROLE_KITCHEN"));
            if (!isStaff) {
                String username = authentication.getName();
                User user = userRepository.findByUsername(username).orElse(null);
                if (user == null || user.getGuest() == null || !reservation.getGuest().getId().equals(user.getGuest().getId())) {
                    throw new AccessDeniedException("No tiene permisos para " + action + " en esta reservación");
                }
            }
        }
    }

    @Override
    public List<RoomOrderResponse> findActiveKitchenOrders() {
        return roomOrderRepository.findByStatusInOrderByOrderedAtAsc(
                List.of(OrderStatus.RECEIVED, OrderStatus.PREPARING, OrderStatus.READY_FOR_DELIVERY, OrderStatus.DELIVERED)
        ).stream()
                .filter(order -> order.getItems().stream().anyMatch(item ->
                        item.getExtraService() != null &&
                        (item.getExtraService().getCategory() == ExtraCategory.GASTRONOMY ||
                         item.getExtraService().getCategory() == ExtraCategory.BAR_WINE)))
                .map(this::mapToResponse)
                .toList();
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
        RoomOrderResponse response = mapToResponse(updated);

        NotificationEvent event = NotificationEvent.of(
                "ORDER_STATUS_CHANGED",
                "ROOM_SERVICE",
                updated.getId(),
                "Comanda #" + updated.getId() + " Actualizada",
                "Nuevo estado: " + status + " en Suite " + updated.getRoomNumber(),
                response
        );
        sseNotificationService.broadcast(event);

        return response;
    }

    @Override
    @Transactional
    public RoomOrderResponse assignTherapist(Long orderId, Long therapistId) {
        RoomOrder order = roomOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Room order not found with id: " + orderId));
        com.hotel.domain.model.Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found with id: " + therapistId));

        if (!Boolean.TRUE.equals(therapist.getActive())) {
            throw new IllegalArgumentException("El especialista " + therapist.getName() + " se encuentra inactivo o en descanso");
        }

        if (order.getScheduledTime() != null && !order.getScheduledTime().isBlank()) {
            boolean isBusy = roomOrderRepository.existsByTherapistIdAndScheduledTimeAndStatusIn(
                    therapistId,
                    order.getScheduledTime(),
                    List.of(OrderStatus.PREPARING, OrderStatus.READY_FOR_DELIVERY)
            );
            if (isBusy) {
                throw new IllegalArgumentException("El especialista " + therapist.getName() + " ya tiene una cita asignada para el horario " + order.getScheduledTime());
            }
        }

        order.setTherapistId(therapist.getId());
        order.setTherapistName(therapist.getName());
        order.setStatus(OrderStatus.PREPARING);

        RoomOrder updated = roomOrderRepository.save(order);
        RoomOrderResponse response = mapToResponse(updated);

        NotificationEvent event = NotificationEvent.of(
                "ORDER_THERAPIST_ASSIGNED",
                "SPA",
                updated.getId(),
                "Terapeuta Asignado",
                therapist.getName() + " asignado a comanda de Suite " + updated.getRoomNumber(),
                response
        );
        sseNotificationService.broadcast(event);

        return response;
    }

    @Override
    public List<Long> findBusyTherapistIds(String scheduledTime) {
        if (scheduledTime == null || scheduledTime.isBlank()) {
            return List.of();
        }
        return roomOrderRepository.findBusyTherapistIdsByScheduledTime(
                scheduledTime,
                List.of(OrderStatus.PREPARING, OrderStatus.READY_FOR_DELIVERY)
        );
    }

    private RoomOrderResponse mapToResponse(RoomOrder order) {
        List<RoomOrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> new RoomOrderItemResponse(
                        item.getId(),
                        item.getExtraService() != null ? item.getExtraService().getId() : null,
                        item.getServiceName(),
                        item.getExtraService() != null ? item.getExtraService().getCategory() : null,
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
                order.getTherapistId(),
                order.getTherapistName(),
                order.getServiceLocation(),
                order.getScheduledTime(),
                itemResponses
        );
    }
}
