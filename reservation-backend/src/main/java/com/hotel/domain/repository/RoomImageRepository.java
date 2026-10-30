package com.hotel.domain.repository;

import com.hotel.domain.model.RoomImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomImageRepository extends JpaRepository<RoomImage, Long> {

    List<RoomImage> findByRoomTypeIdOrderByDisplayOrderAsc(Long roomTypeId);

    Optional<RoomImage> findByRoomTypeIdAndIsCoverTrue(Long roomTypeId);

    boolean existsByRoomTypeIdAndIsCoverTrue(Long roomTypeId);

    @Modifying
    @Query("UPDATE RoomImage r SET r.isCover = false WHERE r.roomType.id = :roomTypeId")
    void resetCoverImagesForRoomType(@Param("roomTypeId") Long roomTypeId);
}
