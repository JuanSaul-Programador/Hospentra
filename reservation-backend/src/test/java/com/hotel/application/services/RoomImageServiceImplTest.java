package com.hotel.application.services;

import com.hotel.application.services.impl.RoomImageServiceImpl;
import com.hotel.domain.exception.ResourceNotFoundException;
import com.hotel.domain.model.RoomImage;
import com.hotel.domain.model.RoomType;
import com.hotel.domain.repository.RoomImageRepository;
import com.hotel.domain.repository.RoomTypeRepository;
import com.hotel.web.dto.RoomImageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomImageServiceImplTest {

    @Mock
    private RoomImageRepository roomImageRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private MediaStorageService mediaStorageService;

    @InjectMocks
    private RoomImageServiceImpl roomImageService;

    private RoomType sampleRoomType;
    private RoomImage sampleCoverImage;

    @BeforeEach
    void setUp() {
        sampleRoomType = RoomType.builder()
                .id(1L)
                .name("Penthouse Imperial")
                .nameEn("Imperial Penthouse")
                .description("Luxury suite")
                .capacity(4)
                .pricePerNight(new BigDecimal("950.00"))
                .coverImageUrl("https://example.com/cover.jpg")
                .build();

        sampleCoverImage = RoomImage.builder()
                .id(10L)
                .roomType(sampleRoomType)
                .imageUrl("https://example.com/cover.jpg")
                .isCover(true)
                .displayOrder(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getImagesByRoomType_ShouldReturnList_WhenRoomTypeExists() {
        when(roomTypeRepository.existsById(1L)).thenReturn(true);
        when(roomImageRepository.findByRoomTypeIdOrderByDisplayOrderAsc(1L))
                .thenReturn(List.of(sampleCoverImage));

        List<RoomImageResponse> result = roomImageService.getImagesByRoomType(1L);

        assertEquals(1, result.size());
        assertEquals("https://example.com/cover.jpg", result.get(0).imageUrl());
        assertTrue(result.get(0).isCover());
    }

    @Test
    void getImagesByRoomType_ShouldThrowException_WhenRoomTypeNotFound() {
        when(roomTypeRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> roomImageService.getImagesByRoomType(99L));
    }

    @Test
    void uploadImage_ShouldSaveAsCover_WhenIsCoverIsTrue() {
        MockMultipartFile file = new MockMultipartFile("file", "suite.jpg", "image/jpeg", "content".getBytes());

        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(sampleRoomType));
        when(mediaStorageService.uploadFile(file, "rooms")).thenReturn("/uploads/rooms/suite.jpg");
        when(roomImageRepository.findByRoomTypeIdOrderByDisplayOrderAsc(1L)).thenReturn(List.of());
        when(roomImageRepository.save(any(RoomImage.class))).thenAnswer(inv -> {
            RoomImage img = inv.getArgument(0);
            img.setId(20L);
            return img;
        });

        RoomImageResponse result = roomImageService.uploadImage(1L, file, true);

        assertNotNull(result);
        assertEquals("/uploads/rooms/suite.jpg", result.imageUrl());
        assertTrue(result.isCover());
        verify(roomImageRepository).resetCoverImagesForRoomType(1L);
        verify(roomTypeRepository).save(sampleRoomType);
    }

    @Test
    void uploadImage_ShouldThrowException_WhenFileExceeds5Mb() {
        byte[] oversized = new byte[(5 * 1024 * 1024) + 1];
        MockMultipartFile file = new MockMultipartFile("file", "big.jpg", "image/jpeg", oversized);

        assertThrows(IllegalArgumentException.class, () -> roomImageService.uploadImage(1L, file, false));
        verifyNoInteractions(mediaStorageService);
    }

    @Test
    void uploadImage_ShouldThrowException_WhenSvgOrHtmlContentType() {
        MockMultipartFile svgFile = new MockMultipartFile("file", "xss.svg", "image/svg+xml", "<svg></svg>".getBytes());
        MockMultipartFile htmlFile = new MockMultipartFile("file", "xss.html", "text/html", "<html></html>".getBytes());

        assertThrows(IllegalArgumentException.class, () -> roomImageService.uploadImage(1L, svgFile, false));
        assertThrows(IllegalArgumentException.class, () -> roomImageService.uploadImage(1L, htmlFile, false));
        verifyNoInteractions(mediaStorageService);
    }

    @Test
    void setCoverImage_ShouldUpdateCover_WhenImageExists() {
        RoomImage referentialImage = RoomImage.builder()
                .id(11L)
                .roomType(sampleRoomType)
                .imageUrl("/uploads/rooms/new-cover.jpg")
                .isCover(false)
                .displayOrder(1)
                .build();

        when(roomImageRepository.findById(11L)).thenReturn(Optional.of(referentialImage));
        when(roomImageRepository.save(any(RoomImage.class))).thenAnswer(inv -> inv.getArgument(0));

        RoomImageResponse result = roomImageService.setCoverImage(11L);

        assertTrue(result.isCover());
        verify(roomImageRepository).resetCoverImagesForRoomType(1L);
        assertEquals("/uploads/rooms/new-cover.jpg", sampleRoomType.getCoverImageUrl());
        verify(roomTypeRepository).save(sampleRoomType);
    }

    @Test
    void deleteImage_ShouldAssignNewCover_WhenCoverIsDeleted() {
        RoomImage nextImage = RoomImage.builder()
                .id(12L)
                .roomType(sampleRoomType)
                .imageUrl("https://example.com/second.jpg")
                .isCover(false)
                .displayOrder(1)
                .build();

        when(roomImageRepository.findById(10L)).thenReturn(Optional.of(sampleCoverImage));
        when(roomImageRepository.findByRoomTypeIdOrderByDisplayOrderAsc(1L))
                .thenReturn(new ArrayList<>(List.of(nextImage)));

        roomImageService.deleteImage(10L);

        verify(roomImageRepository).delete(sampleCoverImage);
        verify(mediaStorageService).deleteFile("https://example.com/cover.jpg");
        assertTrue(nextImage.getIsCover());
        assertEquals("https://example.com/second.jpg", sampleRoomType.getCoverImageUrl());
        verify(roomTypeRepository).save(sampleRoomType);
    }
}
