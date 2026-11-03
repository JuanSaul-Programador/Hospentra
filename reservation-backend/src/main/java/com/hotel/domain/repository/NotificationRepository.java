package com.hotel.domain.repository;

import com.hotel.domain.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndReadStatusFalse(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.readStatus = true WHERE n.user.id = :userId AND n.readStatus = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);
}
