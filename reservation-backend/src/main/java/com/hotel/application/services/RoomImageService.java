package com.hotel.application.services;

import com.hotel.web.dto.RoomImageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RoomImageService {

    List<RoomImageResponse> getImagesByRoomType(Long roomTypeId);

    RoomImageResponse uploadImage(Long roomTypeId, MultipartFile file, boolean isCover);

    RoomImageResponse setCoverImage(Long imageId);

    void deleteImage(Long imageId);
}
