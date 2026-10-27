package com.hotel.application.services.impl;

import com.hotel.application.dto.notification.NotificationEvent;
import com.hotel.application.mapper.RoomMapper;
import com.hotel.application.services.RoomService;
import com.hotel.application.services.SseNotificationService;
import com.hotel.domain.exception.ResourceNotFoundException;
import com.hotel.domain.model.Room;
import com.hotel.domain.model.RoomStatus;
import com.hotel.domain.model.RoomType;
import com.hotel.domain.repository.RoomRepository;
import com.hotel.domain.repository.RoomTypeRepository;
import com.hotel.web.dto.RoomRequest;
import com.hotel.web.dto.RoomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomMapper roomMapper;
    private final SseNotificationService sseNotificationService;

    @Override
    public List<RoomResponse> findAll() {
        return roomRepository.findAll().stream()
                .map(roomMapper::toResponse)
                .toList();
    }

    @Override
    public List<RoomResponse> findByStatus(RoomStatus status) {
        return roomRepository.findByStatus(status).stream()
                .map(roomMapper::toResponse)
                .toList();
    }

    @Override
    public RoomResponse findById(Long id) {
        return roomRepository.findById(id)
                .map(roomMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));
    }

    @Override
    @Transactional
    public RoomResponse create(RoomRequest request) {
        if (roomRepository.existsByRoomNumber(request.roomNumber())) {
            throw new IllegalArgumentException("A room with number '" + request.roomNumber() + "' already exists");
        }
        RoomType roomType = roomTypeRepository.findById(request.roomTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("RoomType not found with id: " + request.roomTypeId()));
        Room room = roomMapper.toEntity(request, roomType);
        return roomMapper.toResponse(roomRepository.save(room));
    }

    @Override
    @Transactional
    public RoomResponse update(Long id, RoomRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));
        RoomType roomType = roomTypeRepository.findById(request.roomTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("RoomType not found with id: " + request.roomTypeId()));
        room.setRoomNumber(request.roomNumber());
        room.setStatus(request.status());
        room.setRoomType(roomType);
        return roomMapper.toResponse(roomRepository.save(room));
    }

    @Override
    @Transactional
    public RoomResponse updateStatus(Long id, RoomStatus status) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));
        room.setStatus(status);
        RoomResponse response = roomMapper.toResponse(roomRepository.save(room));

        NotificationEvent event = NotificationEvent.of(
                "ROOM_STATUS_CHANGED",
                "ROOM_RACK",
                room.getId(),
                "Estado de Suite " + room.getRoomNumber() + " Actualizado",
                "La habitación ahora está " + status,
                response
        );
        sseNotificationService.broadcast(event);

        return response;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!roomRepository.existsById(id)) {
            throw new ResourceNotFoundException("Room not found with id: " + id);
        }
        roomRepository.deleteById(id);
    }
}
