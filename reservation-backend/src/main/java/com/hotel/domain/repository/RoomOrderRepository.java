package com.hotel.domain.repository;

import com.hotel.domain.model.OrderStatus;
import com.hotel.domain.model.RoomOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomOrderRepository extends JpaRepository<RoomOrder, Long> {

    @Override
    @EntityGraph(attributePaths = {"items"})
    Optional<RoomOrder> findById(Long id);

    @EntityGraph(attributePaths = {"items"})
    List<RoomOrder> findByReservationIdOrderByOrderedAtDesc(Long reservationId);

    @EntityGraph(attributePaths = {"items"})
    List<RoomOrder> findByStatusInOrderByOrderedAtAsc(List<OrderStatus> statuses);

    @EntityGraph(attributePaths = {"items"})
    List<RoomOrder> findAllByOrderByOrderedAtDesc();

    boolean existsByTherapistIdAndScheduledTimeAndStatusIn(Long therapistId, String scheduledTime, List<OrderStatus> statuses);

    @Query("SELECT DISTINCT o.therapistId FROM RoomOrder o WHERE o.therapistId IS NOT NULL AND o.scheduledTime = :scheduledTime AND o.status IN :statuses")
    List<Long> findBusyTherapistIdsByScheduledTime(
            @Param("scheduledTime") String scheduledTime,
            @Param("statuses") List<OrderStatus> statuses
    );
}
