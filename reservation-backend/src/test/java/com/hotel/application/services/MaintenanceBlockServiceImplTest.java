package com.hotel.application.services;

import com.hotel.application.dto.notification.NotificationEvent;
import com.hotel.application.services.impl.MaintenanceBlockServiceImpl;
import com.hotel.domain.exception.ResourceNotFoundException;
import com.hotel.domain.model.*;
import com.hotel.domain.repository.MaintenanceBlockRepository;
import com.hotel.domain.repository.ReservationRepository;
import com.hotel.domain.repository.RoomRepository;
import com.hotel.web.dto.MaintenanceBlockRequest;
import com.hotel.web.dto.MaintenanceBlockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceBlockServiceImplTest {

    @Mock
    private MaintenanceBlockRepository maintenanceBlockRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private SseNotificationService sseNotificationService;

    @InjectMocks
    private MaintenanceBlockServiceImpl maintenanceBlockService;

    private Room room;
    private RoomType roomType;

    @BeforeEach
    void setUp() {
        roomType = RoomType.builder().id(1L).name("Royal Suite").pricePerNight(new BigDecimal("350.00")).build();
        room = Room.builder().id(10L).roomNumber("201").roomType(roomType).status(RoomStatus.AVAILABLE).build();

        var auth = new UsernamePasswordAuthenticationToken("admin1", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void createBlock_Success_ShouldPersistAndAudit() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(4);
        MaintenanceBlockRequest request = new MaintenanceBlockRequest(
                10L, BlockType.OUT_OF_ORDER, start, end, "Fuga en jacuzzi", "Plomería Express"
        );

        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(reservationRepository.existsOverlappingReservation(eq(10L), eq(start), eq(end), anyList())).thenReturn(false);
        when(maintenanceBlockRepository.existsOverlappingActiveBlock(eq(10L), eq(start), eq(end))).thenReturn(false);

        MaintenanceBlock savedBlock = MaintenanceBlock.builder()
                .id(1L)
                .room(room)
                .blockType(BlockType.OUT_OF_ORDER)
                .startDate(start)
                .endDate(end)
                .reason("Fuga en jacuzzi")
                .contractorName("Plomería Express")
                .status(BlockStatus.ACTIVE)
                .createdBy("admin1")
                .createdAt(LocalDateTime.now())
                .build();

        when(maintenanceBlockRepository.save(any(MaintenanceBlock.class))).thenReturn(savedBlock);

        MaintenanceBlockResponse response = maintenanceBlockService.createBlock(request);

        assertThat(response).isNotNull();
        assertThat(response.getRoomNumber()).isEqualTo("201");
        assertThat(response.getBlockType()).isEqualTo(BlockType.OUT_OF_ORDER);
        assertThat(response.getDurationDays()).isEqualTo(4);

        verify(auditLogService).logAction(eq("CREATE_MAINTENANCE_BLOCK"), eq("Room"), eq("201"), anyString(), eq("SUCCESS"));
        verify(sseNotificationService).broadcast(any(NotificationEvent.class));
    }

    @Test
    void createBlock_ActiveToday_ShouldUpdateRoomStatusToMaintenance() {
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(3);
        MaintenanceBlockRequest request = new MaintenanceBlockRequest(
                10L, BlockType.OUT_OF_ORDER, today, end, "Aire acondicionado averiado", null
        );

        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(reservationRepository.existsOverlappingReservation(eq(10L), eq(today), eq(end), anyList())).thenReturn(false);
        when(maintenanceBlockRepository.existsOverlappingActiveBlock(eq(10L), eq(today), eq(end))).thenReturn(false);

        MaintenanceBlock savedBlock = MaintenanceBlock.builder()
                .id(2L)
                .room(room)
                .blockType(BlockType.OUT_OF_ORDER)
                .startDate(today)
                .endDate(end)
                .reason("Aire acondicionado averiado")
                .status(BlockStatus.ACTIVE)
                .createdBy("admin1")
                .createdAt(LocalDateTime.now())
                .build();

        when(maintenanceBlockRepository.save(any(MaintenanceBlock.class))).thenReturn(savedBlock);

        maintenanceBlockService.createBlock(request);

        assertThat(room.getStatus()).isEqualTo(RoomStatus.MAINTENANCE);
        verify(roomRepository).save(room);
    }

    @Test
    void createBlock_WhenInvertedDates_ThrowsIllegalArgument() {
        LocalDate start = LocalDate.now().plusDays(5);
        LocalDate end = LocalDate.now().plusDays(2);
        MaintenanceBlockRequest request = new MaintenanceBlockRequest(
                10L, BlockType.OUT_OF_ORDER, start, end, "Test", null
        );

        assertThatThrownBy(() -> maintenanceBlockService.createBlock(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha de inicio no puede ser posterior");
    }

    @Test
    void createBlock_WhenPastEndDate_ThrowsIllegalArgument() {
        LocalDate start = LocalDate.now().minusDays(5);
        LocalDate end = LocalDate.now().minusDays(2);
        MaintenanceBlockRequest request = new MaintenanceBlockRequest(
                10L, BlockType.OUT_OF_ORDER, start, end, "Test", null
        );

        assertThatThrownBy(() -> maintenanceBlockService.createBlock(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fechas ya transcurridas");
    }

    @Test
    void createBlock_WhenReservationOverlapExists_ThrowsIllegalState() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(3);
        MaintenanceBlockRequest request = new MaintenanceBlockRequest(
                10L, BlockType.OUT_OF_ORDER, start, end, "Test", null
        );

        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(reservationRepository.existsOverlappingReservation(eq(10L), eq(start), eq(end), anyList())).thenReturn(true);

        assertThatThrownBy(() -> maintenanceBlockService.createBlock(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tiene reservaciones confirmadas o en curso");
    }

    @Test
    void createBlock_WhenBlockOverlapExists_ThrowsIllegalState() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(3);
        MaintenanceBlockRequest request = new MaintenanceBlockRequest(
                10L, BlockType.OUT_OF_ORDER, start, end, "Test", null
        );

        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(reservationRepository.existsOverlappingReservation(eq(10L), eq(start), eq(end), anyList())).thenReturn(false);
        when(maintenanceBlockRepository.existsOverlappingActiveBlock(eq(10L), eq(start), eq(end))).thenReturn(true);

        assertThatThrownBy(() -> maintenanceBlockService.createBlock(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya existe un bloqueo de mantenimiento activo");
    }

    @Test
    void resolveBlock_Success_ShouldMarkResolvedAndRestoreRoom() {
        room.setStatus(RoomStatus.MAINTENANCE);
        MaintenanceBlock block = MaintenanceBlock.builder()
                .id(5L)
                .room(room)
                .blockType(BlockType.OUT_OF_ORDER)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(2))
                .status(BlockStatus.ACTIVE)
                .build();

        when(maintenanceBlockRepository.findById(5L)).thenReturn(Optional.of(block));
        when(maintenanceBlockRepository.save(any(MaintenanceBlock.class))).thenReturn(block);
        when(maintenanceBlockRepository.existsOverlappingActiveBlock(eq(10L), any(), any())).thenReturn(false);

        MaintenanceBlockResponse response = maintenanceBlockService.resolveBlock(5L);

        assertThat(response.getStatus()).isEqualTo(BlockStatus.RESOLVED);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.AVAILABLE);
        verify(roomRepository).save(room);
        verify(auditLogService).logAction(eq("RESOLVE_MAINTENANCE_BLOCK"), eq("Room"), eq("201"), anyString(), eq("SUCCESS"));
    }

    @Test
    void cancelBlock_Success_ShouldMarkCancelled() {
        MaintenanceBlock block = MaintenanceBlock.builder()
                .id(6L)
                .room(room)
                .blockType(BlockType.OUT_OF_SERVICE)
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(5))
                .status(BlockStatus.ACTIVE)
                .build();

        when(maintenanceBlockRepository.findById(6L)).thenReturn(Optional.of(block));
        when(maintenanceBlockRepository.save(any(MaintenanceBlock.class))).thenReturn(block);

        MaintenanceBlockResponse response = maintenanceBlockService.cancelBlock(6L);

        assertThat(response.getStatus()).isEqualTo(BlockStatus.CANCELLED);
        verify(auditLogService).logAction(eq("CANCEL_MAINTENANCE_BLOCK"), eq("Room"), eq("201"), anyString(), eq("SUCCESS"));
    }

    @Test
    void getActiveBlocks_ShouldReturnList() {
        MaintenanceBlock block = MaintenanceBlock.builder()
                .id(7L)
                .room(room)
                .blockType(BlockType.OUT_OF_ORDER)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .status(BlockStatus.ACTIVE)
                .build();

        when(maintenanceBlockRepository.findActiveBlocksInDateRange(any(), any())).thenReturn(List.of(block));

        List<MaintenanceBlockResponse> result = maintenanceBlockService.getActiveBlocks(null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRoomNumber()).isEqualTo("201");
    }
}
