package com.hotel.domain.repository;

import com.hotel.domain.model.OrderStatus;
import com.hotel.domain.model.RoomOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomOrderRepository extends JpaRepository<RoomOrder, Long> {
    List<RoomOrder> findByReservationIdOrderByOrderedAtDesc(Long reservationId);
    List<RoomOrder> findByStatusInOrderByOrderedAtAsc(List<OrderStatus> statuses);
    List<RoomOrder> findAllByOrderByOrderedAtDesc();
}
