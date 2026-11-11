package com.hotel.application.services.impl;

import com.hotel.application.dto.notification.NotificationEvent;
import com.hotel.application.services.AuditLogService;
import com.hotel.application.services.MaintenanceBlockService;
import com.hotel.application.services.SseNotificationService;
import com.hotel.domain.exception.ResourceNotFoundException;
import com.hotel.domain.model.*;
import com.hotel.domain.repository.MaintenanceBlockRepository;
import com.hotel.domain.repository.ReservationRepository;
import com.hotel.domain.repository.RoomRepository;
import com.hotel.web.dto.MaintenanceBlockRequest;
import com.hotel.web.dto.MaintenanceBlockResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceBlockServiceImpl implements MaintenanceBlockService {

    private final MaintenanceBlockRepository maintenanceBlockRepository;
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final AuditLogService auditLogService;
    private final SseNotificationService sseNotificationService;

    @Override
    @Transactional
    public MaintenanceBlockResponse createBlock(MaintenanceBlockRequest request) {
        if (request.startDate().isAfter(request.endDate())) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de finalización");
        }

        if (request.endDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("No se pueden programar bloqueos en fechas ya transcurridas");
        }

        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Habitación no encontrada con ID: " + request.roomId()));

        List<ReservationStatus> activeReservationStatuses = List.of(
                ReservationStatus.PENDING,
                ReservationStatus.CONFIRMED,
                ReservationStatus.CHECKED_IN
        );

        boolean hasReservationOverlap = reservationRepository.existsOverlappingReservation(
                room.getId(), request.startDate(), request.endDate(), activeReservationStatuses
        );

        if (hasReservationOverlap) {
            throw new IllegalStateException("No es posible programar el bloqueo: la suite " + room.getRoomNumber() +
                    " tiene reservaciones confirmadas o en curso en el rango de fechas seleccionado.");
        }

        boolean hasBlockOverlap = maintenanceBlockRepository.existsOverlappingActiveBlock(
                room.getId(), request.startDate(), request.endDate()
        );

        if (hasBlockOverlap) {
            throw new IllegalStateException("Ya existe un bloqueo de mantenimiento activo para la suite " +
                    room.getRoomNumber() + " en las fechas indicadas.");
        }

        String username = "system";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            username = auth.getName();
        }

        MaintenanceBlock block = MaintenanceBlock.builder()
                .room(room)
                .blockType(request.blockType())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .reason(request.reason().trim())
                .contractorName(request.contractorName() != null && !request.contractorName().isBlank() ? request.contractorName().trim() : null)
                .status(BlockStatus.ACTIVE)
                .createdBy(username)
                .createdAt(LocalDateTime.now())
                .build();

        MaintenanceBlock saved = maintenanceBlockRepository.save(block);

        LocalDate today = LocalDate.now();
        if (!request.startDate().isAfter(today) && !request.endDate().isBefore(today)) {
            room.setStatus(RoomStatus.MAINTENANCE);
            roomRepository.save(room);
        }

        auditLogService.logAction(
                "CREATE_MAINTENANCE_BLOCK",
                "Room",
                room.getRoomNumber(),
                "Bloqueo " + request.blockType() + " programado del " + request.startDate() + " al " + request.endDate() + ": " + request.reason().trim(),
                "SUCCESS"
        );

        MaintenanceBlockResponse response = mapToResponse(saved);

        NotificationEvent event = NotificationEvent.of(
                "ROOM_STATUS_CHANGED",
                "FRONT_DESK",
                room.getId(),
                "Bloqueo de Mantenimiento Suite #" + room.getRoomNumber(),
                "Suite " + room.getRoomNumber() + " inhabilitada (" + request.blockType() + ") del " + request.startDate() + " al " + request.endDate(),
                response
        );
        sseNotificationService.sendToRole("ROLE_ADMIN", event);
        sseNotificationService.sendToRole("ROLE_RECEPTIONIST", event);

        return response;
    }

    @Override
    @Transactional
    public MaintenanceBlockResponse resolveBlock(Long id) {
        MaintenanceBlock block = maintenanceBlockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bloqueo de mantenimiento no encontrado con ID: " + id));

        block.setStatus(BlockStatus.RESOLVED);
        block.setResolvedAt(LocalDateTime.now());
        MaintenanceBlock saved = maintenanceBlockRepository.save(block);

        Room room = block.getRoom();
        LocalDate today = LocalDate.now();
        boolean stillBlocked = maintenanceBlockRepository.existsOverlappingActiveBlock(room.getId(), today, today);

        if (!stillBlocked && room.getStatus() == RoomStatus.MAINTENANCE) {
            room.setStatus(RoomStatus.AVAILABLE);
            roomRepository.save(room);
        }

        auditLogService.logAction(
                "RESOLVE_MAINTENANCE_BLOCK",
                "Room",
                room.getRoomNumber(),
                "Bloqueo de mantenimiento #" + block.getId() + " concluido/resuelto exitosamente",
                "SUCCESS"
        );

        MaintenanceBlockResponse response = mapToResponse(saved);

        NotificationEvent event = NotificationEvent.of(
                "ROOM_STATUS_CHANGED",
                "FRONT_DESK",
                room.getId(),
                "Mantenimiento Concluido Suite #" + room.getRoomNumber(),
                "El bloqueo de la suite " + room.getRoomNumber() + " fue levantado y la habitación vuelve a estar disponible",
                response
        );
        sseNotificationService.sendToRole("ROLE_ADMIN", event);
        sseNotificationService.sendToRole("ROLE_RECEPTIONIST", event);

        return response;
    }

    @Override
    @Transactional
    public MaintenanceBlockResponse cancelBlock(Long id) {
        MaintenanceBlock block = maintenanceBlockRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bloqueo de mantenimiento no encontrado con ID: " + id));

        block.setStatus(BlockStatus.CANCELLED);
        block.setResolvedAt(LocalDateTime.now());
        MaintenanceBlock saved = maintenanceBlockRepository.save(block);

        Room room = block.getRoom();
        LocalDate today = LocalDate.now();
        boolean stillBlocked = maintenanceBlockRepository.existsOverlappingActiveBlock(room.getId(), today, today);

        if (!stillBlocked && room.getStatus() == RoomStatus.MAINTENANCE) {
            room.setStatus(RoomStatus.AVAILABLE);
            roomRepository.save(room);
        }

        auditLogService.logAction(
                "CANCEL_MAINTENANCE_BLOCK",
                "Room",
                room.getRoomNumber(),
                "Bloqueo de mantenimiento #" + block.getId() + " cancelado",
                "SUCCESS"
        );

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceBlockResponse> getActiveBlocks(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now().minusDays(15);
        LocalDate end = endDate != null ? endDate : LocalDate.now().plusDays(30);

        return maintenanceBlockRepository.findActiveBlocksInDateRange(start, end)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceBlockResponse> getAllBlocks() {
        return maintenanceBlockRepository.findAllWithRoom()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceBlockResponse> getBlocksByRoom(Long roomId) {
        return maintenanceBlockRepository.findByRoomIdOrderByStartDateDesc(roomId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private MaintenanceBlockResponse mapToResponse(MaintenanceBlock block) {
        long duration = (block.getStartDate() != null && block.getEndDate() != null)
                ? Math.max(1, ChronoUnit.DAYS.between(block.getStartDate(), block.getEndDate()) + 1)
                : 1;

        return MaintenanceBlockResponse.builder()
                .id(block.getId())
                .roomId(block.getRoom().getId())
                .roomNumber(block.getRoom().getRoomNumber())
                .roomTypeName(block.getRoom().getRoomType() != null ? block.getRoom().getRoomType().getName() : "")
                .blockType(block.getBlockType())
                .startDate(block.getStartDate())
                .endDate(block.getEndDate())
                .reason(block.getReason())
                .contractorName(block.getContractorName())
                .status(block.getStatus())
                .createdBy(block.getCreatedBy())
                .resolvedAt(block.getResolvedAt())
                .createdAt(block.getCreatedAt())
                .durationDays(duration)
                .build();
    }
}
