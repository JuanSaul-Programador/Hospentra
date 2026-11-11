package com.hotel.application.services;

import com.hotel.application.dto.notification.NotificationEvent;
import com.hotel.application.mapper.RoomMapper;
import com.hotel.application.mapper.RoomTypeMapper;
import com.hotel.application.services.impl.RoomServiceImpl;
import com.hotel.domain.exception.ResourceNotFoundException;
import com.hotel.domain.model.Room;
import com.hotel.domain.model.RoomStatus;
import com.hotel.domain.model.RoomType;
import com.hotel.domain.repository.RoomRepository;
import com.hotel.domain.repository.RoomTypeRepository;
import com.hotel.web.dto.RoomRequest;
import com.hotel.web.dto.RoomResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private SseNotificationService sseNotificationService;

    private RoomServiceImpl roomService;

    private RoomType luxuryType;
    private Room luxuryRoom;

    @BeforeEach
    void setUp() {
        RoomMapper roomMapper = new RoomMapper(new RoomTypeMapper());
        roomService = new RoomServiceImpl(
                roomRepository,
                roomTypeRepository,
                roomMapper,
                sseNotificationService
        );

        luxuryType = RoomType.builder()
                .id(1L)
                .name("Penthouse Presidencial")
                .nameEn("Presidential Penthouse")
                .description("Vista panorámica")
                .descriptionEn("Panoramic view")
                .capacity(4)
                .pricePerNight(new BigDecimal("500.00"))
                .build();

        luxuryRoom = Room.builder()
                .id(201L)
                .roomNumber("201")
                .status(RoomStatus.MAINTENANCE)
                .roomType(luxuryType)
                .build();
    }

    @Test
    void findAllReturnsAllRooms() {
        when(roomRepository.findAll()).thenReturn(List.of(luxuryRoom));

        List<RoomResponse> results = roomService.findAll();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).roomNumber()).isEqualTo("201");
        assertThat(results.get(0).roomType().nameEn()).isEqualTo("Presidential Penthouse");
    }

    @Test
    void findByStatusReturnsFilteredRooms() {
        when(roomRepository.findByStatus(RoomStatus.MAINTENANCE)).thenReturn(List.of(luxuryRoom));

        List<RoomResponse> results = roomService.findByStatus(RoomStatus.MAINTENANCE);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).status()).isEqualTo(RoomStatus.MAINTENANCE);
    }

    @Test
    void findByIdSuccessWhenExists() {
        when(roomRepository.findById(201L)).thenReturn(Optional.of(luxuryRoom));

        RoomResponse response = roomService.findById(201L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(201L);
        assertThat(response.roomNumber()).isEqualTo("201");
    }

    @Test
    void findByIdFailsWhenNotFound() {
        when(roomRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Room not found with id: 999");
    }

    @Test
    void createRoomSuccessWithAssignedRoomType() {
        RoomRequest request = new RoomRequest("305", RoomStatus.AVAILABLE, 1L);

        when(roomRepository.existsByRoomNumber("305")).thenReturn(false);
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(luxuryType));
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> {
            Room entity = invocation.getArgument(0);
            entity.setId(305L);
            return entity;
        });

        RoomResponse response = roomService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(305L);
        assertThat(response.roomNumber()).isEqualTo("305");
        assertThat(response.status()).isEqualTo(RoomStatus.AVAILABLE);
        assertThat(response.roomType().id()).isEqualTo(1L);
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    void createRoomFailsWhenRoomNumberAlreadyExists() {
        RoomRequest request = new RoomRequest("201", RoomStatus.AVAILABLE, 1L);

        when(roomRepository.existsByRoomNumber("201")).thenReturn(true);

        assertThatThrownBy(() -> roomService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(roomRepository, never()).save(any());
    }

    @Test
    void createRoomFailsWhenRoomTypeNotFound() {
        RoomRequest request = new RoomRequest("401", RoomStatus.AVAILABLE, 99L);

        when(roomRepository.existsByRoomNumber("401")).thenReturn(false);
        when(roomTypeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("RoomType not found with id: 99");

        verify(roomRepository, never()).save(any());
    }

    @Test
    void updateRoomSuccess() {
        RoomRequest request = new RoomRequest("201-B", RoomStatus.AVAILABLE, 1L);

        when(roomRepository.findById(201L)).thenReturn(Optional.of(luxuryRoom));
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(luxuryType));
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoomResponse response = roomService.update(201L, request);

        assertThat(response).isNotNull();
        assertThat(response.roomNumber()).isEqualTo("201-B");
        assertThat(response.status()).isEqualTo(RoomStatus.AVAILABLE);
        assertThat(response.roomType().id()).isEqualTo(1L);
    }

    @Test
    void updateRoomFailsWhenRoomNotFound() {
        RoomRequest request = new RoomRequest("201", RoomStatus.AVAILABLE, 1L);

        when(roomRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.update(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Room not found with id: 999");
    }

    @Test
    void updateRoomFailsWhenRoomTypeNotFound() {
        RoomRequest request = new RoomRequest("201", RoomStatus.AVAILABLE, 999L);

        when(roomRepository.findById(201L)).thenReturn(Optional.of(luxuryRoom));
        when(roomTypeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.update(201L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("RoomType not found with id: 999");
    }

    @Test
    void updateStatusSuccessTransitionsRoomAndBroadcasts() {
        when(roomRepository.findById(201L)).thenReturn(Optional.of(luxuryRoom));
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));

        RoomResponse response = roomService.updateStatus(201L, RoomStatus.AVAILABLE);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(RoomStatus.AVAILABLE);
        assertThat(luxuryRoom.getStatus()).isEqualTo(RoomStatus.AVAILABLE);
        verify(sseNotificationService, times(1)).sendToRole(eq("ROLE_ADMIN"), any(NotificationEvent.class));
        verify(sseNotificationService, times(1)).sendToRole(eq("ROLE_RECEPTIONIST"), any(NotificationEvent.class));
        verify(sseNotificationService, never()).broadcast(any(NotificationEvent.class));
    }

    @Test
    void updateStatusFailsWhenRoomNotFound() {
        when(roomRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.updateStatus(999L, RoomStatus.AVAILABLE))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Room not found");
    }

    @Test
    void deleteRoomSuccess() {
        when(roomRepository.existsById(201L)).thenReturn(true);

        roomService.delete(201L);

        verify(roomRepository, times(1)).deleteById(201L);
    }

    @Test
    void deleteRoomFailsWhenNotFound() {
        when(roomRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> roomService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Room not found with id: 999");

        verify(roomRepository, never()).deleteById(anyLong());
    }
}
