package com.hotel.application.services;

import com.hotel.domain.model.OrderStatus;
import com.hotel.web.dto.CreateRoomOrderRequest;
import com.hotel.web.dto.RoomOrderResponse;

import java.util.List;

public interface RoomOrderService {
    RoomOrderResponse createOrder(CreateRoomOrderRequest request);
    List<RoomOrderResponse> findByReservationId(Long reservationId);
    List<RoomOrderResponse> findActiveKitchenOrders();
    List<RoomOrderResponse> findAllOrders();
    RoomOrderResponse updateOrderStatus(Long orderId, OrderStatus status);
    RoomOrderResponse assignTherapist(Long orderId, Long therapistId);
}
