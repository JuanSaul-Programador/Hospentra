package com.hotel.web.controller;

import com.hotel.application.services.RoomImageService;
import com.hotel.web.dto.RoomImageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RoomImageController {

    private final RoomImageService roomImageService;

    @GetMapping("/api/room-types/{roomTypeId}/images")
    public ResponseEntity<List<RoomImageResponse>> getImagesByRoomType(@PathVariable Long roomTypeId) {
        return ResponseEntity.ok(roomImageService.getImagesByRoomType(roomTypeId));
    }

    @PostMapping(value = "/api/room-types/{roomTypeId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RoomImageResponse> uploadImage(
            @PathVariable Long roomTypeId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isCover", defaultValue = "false") boolean isCover) {
        RoomImageResponse response = roomImageService.uploadImage(roomTypeId, file, isCover);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/api/room-images/{imageId}/set-cover")
    public ResponseEntity<RoomImageResponse> setCoverImage(@PathVariable Long imageId) {
        return ResponseEntity.ok(roomImageService.setCoverImage(imageId));
    }

    @DeleteMapping("/api/room-images/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long imageId) {
        roomImageService.deleteImage(imageId);
        return ResponseEntity.noContent().build();
    }
}
