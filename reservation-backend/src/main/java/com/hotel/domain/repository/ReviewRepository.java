package com.hotel.domain.repository;

import com.hotel.domain.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByReservationId(Long reservationId);

    boolean existsByReservationId(Long reservationId);

    Page<Review> findByRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);

    Page<Review> findByRoomIdAndRatingOrderByCreatedAtDesc(Long roomId, Integer rating, Pageable pageable);

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.room.id = :roomId GROUP BY r.rating")
    List<Object[]> countRatingsByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.room.id = :roomId")
    Double calculateAverageRatingByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT AVG(r.cleanlinessRating), AVG(r.serviceRating), AVG(r.comfortRating), AVG(r.locationRating) FROM Review r WHERE r.room.id = :roomId")
    List<Object[]> calculateDimensionalAverages(@Param("roomId") Long roomId);

    Long countByRoomId(Long roomId);

    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT r FROM Review r WHERE " +
           "(:rating IS NULL OR r.rating = :rating) AND " +
           "(:unansweredOnly IS NULL OR (:unansweredOnly = true AND r.managementResponse IS NULL) OR (:unansweredOnly = false AND r.managementResponse IS NOT NULL)) " +
           "ORDER BY r.createdAt DESC")
    Page<Review> findAdminReviews(@Param("rating") Integer rating, @Param("unansweredOnly") Boolean unansweredOnly, Pageable pageable);

    long countByManagementResponseIsNull();

    long countByRatingGreaterThanEqual(Integer rating);

    @Query("SELECT AVG(r.rating) FROM Review r")
    Double calculateOverallAverageRating();
}
