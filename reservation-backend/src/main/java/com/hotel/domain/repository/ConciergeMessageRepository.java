package com.hotel.domain.repository;

import com.hotel.domain.model.ConciergeMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConciergeMessageRepository extends JpaRepository<ConciergeMessage, Long> {

    @Query("SELECT m FROM ConciergeMessage m LEFT JOIN FETCH m.sender LEFT JOIN FETCH m.recipient LEFT JOIN FETCH m.reservation WHERE m.sender.id = :userId OR m.recipient.id = :userId ORDER BY m.createdAt ASC")
    List<ConciergeMessage> findConversationByUserId(@Param("userId") Long userId);

    @Query("SELECT m FROM ConciergeMessage m LEFT JOIN FETCH m.sender LEFT JOIN FETCH m.recipient LEFT JOIN FETCH m.reservation WHERE m.reservation.id = :reservationId ORDER BY m.createdAt ASC")
    List<ConciergeMessage> findConversationByReservationId(@Param("reservationId") Long reservationId);

    @Query("SELECT m FROM ConciergeMessage m LEFT JOIN FETCH m.sender LEFT JOIN FETCH m.recipient LEFT JOIN FETCH m.reservation WHERE m.sender.id = :guestId OR m.recipient.id = :guestId ORDER BY m.createdAt ASC")
    List<ConciergeMessage> findConversationBetweenStaffAndGuest(@Param("guestId") Long guestId);

    long countByRecipientIdAndReadStatusFalse(Long recipientId);

    @Query("SELECT COUNT(m) FROM ConciergeMessage m WHERE m.senderRole = com.hotel.domain.model.Role.ROLE_CUSTOMER AND m.readStatus = false")
    long countUnreadForFrontDesk();

    @Modifying
    @Query("UPDATE ConciergeMessage m SET m.readStatus = true WHERE m.recipient.id = :userId AND m.readStatus = false")
    int markAllAsReadForRecipient(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE ConciergeMessage m SET m.readStatus = true WHERE m.sender.id = :guestId AND m.senderRole = com.hotel.domain.model.Role.ROLE_CUSTOMER AND m.readStatus = false")
    int markAllAsReadFromGuest(@Param("guestId") Long guestId);

    @Query("SELECT DISTINCT m.sender.id FROM ConciergeMessage m WHERE m.senderRole = com.hotel.domain.model.Role.ROLE_CUSTOMER")
    List<Long> findDistinctGuestSenderIds();
}
