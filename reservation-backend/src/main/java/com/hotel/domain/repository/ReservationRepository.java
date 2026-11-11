package com.hotel.domain.repository;

import com.hotel.domain.model.Reservation;
import com.hotel.domain.model.ReservationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Override
    @EntityGraph(attributePaths = {"guest", "room", "room.roomType"})
    List<Reservation> findAll();

    @Override
    @EntityGraph(attributePaths = {"guest", "room", "room.roomType"})
    Optional<Reservation> findById(Long id);

    @EntityGraph(attributePaths = {"guest", "room", "room.roomType"})
    List<Reservation> findByStatus(ReservationStatus status);

    @EntityGraph(attributePaths = {"guest", "room", "room.roomType"})
    List<Reservation> findByGuestId(Long guestId);

    boolean existsByGuestIdAndStatusIn(Long guestId, List<ReservationStatus> statuses);

    boolean existsByPaymentIntentId(String paymentIntentId);

    boolean existsByPaymentIntentIdAndIdNot(String paymentIntentId, Long id);

    @Query("SELECT COUNT(r) > 0 FROM Reservation r " +
           "WHERE r.room.id = :roomId " +
           "AND r.status IN :activeStatuses " +
           "AND r.checkInDate < :checkOutDate " +
           "AND r.checkOutDate > :checkInDate")
    boolean existsOverlappingReservation(
            @Param("roomId") Long roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("activeStatuses") List<ReservationStatus> activeStatuses);
}
