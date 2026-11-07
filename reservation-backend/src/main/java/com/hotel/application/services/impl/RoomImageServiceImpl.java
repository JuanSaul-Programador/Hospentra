package com.hotel.application.services.impl;

import com.hotel.application.services.MediaStorageService;
import com.hotel.application.services.RoomImageService;
import com.hotel.domain.exception.ResourceNotFoundException;
import com.hotel.domain.model.RoomImage;
import com.hotel.domain.model.RoomType;
import com.hotel.domain.repository.RoomImageRepository;
import com.hotel.domain.repository.RoomTypeRepository;
import com.hotel.web.dto.RoomImageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomImageServiceImpl implements RoomImageService {

    private final RoomImageRepository roomImageRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final MediaStorageService mediaStorageService;

    @Override
    @Transactional(readOnly = true)
    public List<RoomImageResponse> getImagesByRoomType(Long roomTypeId) {
        if (!roomTypeRepository.existsById(roomTypeId)) {
            throw new ResourceNotFoundException("Tipo de habitación no encontrado con ID: " + roomTypeId);
        }

        return roomImageRepository.findByRoomTypeIdOrderByDisplayOrderAsc(roomTypeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public RoomImageResponse uploadImage(Long roomTypeId, MultipartFile file, boolean isCover) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo de imagen no puede estar vacío");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("El tamaño máximo permitido para imágenes es de 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equalsIgnoreCase("image/jpeg") && !contentType.equalsIgnoreCase("image/png") && !contentType.equalsIgnoreCase("image/webp"))) {
            throw new IllegalArgumentException("Solo se permiten formatos de imagen JPEG, PNG y WEBP");
        }

        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de habitación no encontrado con ID: " + roomTypeId));

        String uploadedUrl = mediaStorageService.uploadFile(file, "rooms");

        boolean shouldBeCover = isCover || !roomImageRepository.existsByRoomTypeIdAndIsCoverTrue(roomTypeId);

        if (shouldBeCover) {
            roomImageRepository.resetCoverImagesForRoomType(roomTypeId);
            roomType.setCoverImageUrl(uploadedUrl);
            roomTypeRepository.save(roomType);
        }

        List<RoomImage> existing = roomImageRepository.findByRoomTypeIdOrderByDisplayOrderAsc(roomTypeId);
        int nextOrder = existing.isEmpty() ? 0 : existing.size();

        RoomImage roomImage = RoomImage.builder()
                .roomType(roomType)
                .imageUrl(uploadedUrl)
                .isCover(shouldBeCover)
                .displayOrder(nextOrder)
                .build();

        RoomImage saved = roomImageRepository.save(roomImage);
        log.info("Room image persisted with ID: {} for RoomType: {}", saved.getId(), roomTypeId);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public RoomImageResponse setCoverImage(Long imageId) {
        RoomImage image = roomImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Imagen no encontrada con ID: " + imageId));

        RoomType roomType = image.getRoomType();
        roomImageRepository.resetCoverImagesForRoomType(roomType.getId());

        image.setIsCover(true);
        RoomImage updated = roomImageRepository.save(image);

        roomType.setCoverImageUrl(image.getImageUrl());
        roomTypeRepository.save(roomType);

        log.info("Image ID: {} set as primary cover for RoomType ID: {}", imageId, roomType.getId());
        return toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteImage(Long imageId) {
        RoomImage image = roomImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Imagen no encontrada con ID: " + imageId));

        RoomType roomType = image.getRoomType();
        boolean wasCover = Boolean.TRUE.equals(image.getIsCover());
        String fileUrl = image.getImageUrl();

        roomImageRepository.delete(image);
        mediaStorageService.deleteFile(fileUrl);

        if (wasCover) {
            List<RoomImage> remaining = roomImageRepository.findByRoomTypeIdOrderByDisplayOrderAsc(roomType.getId());
            if (!remaining.isEmpty()) {
                RoomImage newCover = remaining.get(0);
                newCover.setIsCover(true);
                roomImageRepository.save(newCover);
                roomType.setCoverImageUrl(newCover.getImageUrl());
            } else {
                roomType.setCoverImageUrl(null);
            }
            roomTypeRepository.save(roomType);
        }

        log.info("Image ID: {} successfully removed from RoomType ID: {}", imageId, roomType.getId());
    }

    private RoomImageResponse toResponse(RoomImage image) {
        return new RoomImageResponse(
                image.getId(),
                image.getRoomType().getId(),
                image.getImageUrl(),
                image.getIsCover(),
                image.getDisplayOrder(),
                image.getCreatedAt()
        );
    }
}
