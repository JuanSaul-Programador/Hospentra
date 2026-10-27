package com.hotel.domain.repository;

import com.hotel.domain.model.OrderStatus;
import com.hotel.domain.model.RoomOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomOrderRepository extends JpaRepository<RoomOrder, Long> {
    List<RoomOrder> findByReservationIdOrderByOrderedAtDesc(Long reservationId);
    List<RoomOrder> findByStatusInOrderByOrderedAtAsc(List<OrderStatus> statuses);
    List<RoomOrder> findAllByOrderByOrderedAtDesc();
    boolean existsByTherapistIdAndScheduledTimeAndStatusIn(Long therapistId, String scheduledTime, List<OrderStatus> statuses);

    @Query("SELECT DISTINCT o.therapistId FROM RoomOrder o WHERE o.therapistId IS NOT NULL AND o.scheduledTime = :scheduledTime AND o.status IN :statuses")
    List<Long> findBusyTherapistIdsByScheduledTime(
            @Param("scheduledTime") String scheduledTime,
            @Param("statuses") List<OrderStatus> statuses
    );
}
