package com.hotel.domain.repository;

import com.hotel.domain.model.BlockStatus;
import com.hotel.domain.model.MaintenanceBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MaintenanceBlockRepository extends JpaRepository<MaintenanceBlock, Long> {

    @Query("SELECT COUNT(b) > 0 FROM MaintenanceBlock b " +
           "WHERE b.room.id = :roomId " +
           "AND b.status = 'ACTIVE' " +
           "AND b.startDate <= :checkOutDate " +
           "AND b.endDate >= :checkInDate")
    boolean existsOverlappingActiveBlock(
            @Param("roomId") Long roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate);

    @Query("SELECT b FROM MaintenanceBlock b " +
           "JOIN FETCH b.room r " +
           "JOIN FETCH r.roomType " +
           "WHERE b.status = 'ACTIVE' " +
           "AND b.startDate <= :endDate " +
           "AND b.endDate >= :startDate " +
           "ORDER BY b.startDate ASC")
    List<MaintenanceBlock> findActiveBlocksInDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT b FROM MaintenanceBlock b " +
           "JOIN FETCH b.room r " +
           "JOIN FETCH r.roomType " +
           "ORDER BY b.startDate DESC")
    List<MaintenanceBlock> findAllWithRoom();

    List<MaintenanceBlock> findByRoomIdOrderByStartDateDesc(Long roomId);

    List<MaintenanceBlock> findByStatusOrderByStartDateDesc(BlockStatus status);
}
