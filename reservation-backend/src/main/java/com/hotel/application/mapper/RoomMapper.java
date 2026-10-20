package com.hotel.application.mapper;

import com.hotel.domain.model.Room;
import com.hotel.domain.model.RoomType;
import com.hotel.web.dto.RoomRequest;
import com.hotel.web.dto.RoomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomMapper {

    private final RoomTypeMapper roomTypeMapper;

    public RoomResponse toResponse(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getRoomNumber(),
                room.getStatus(),
                roomTypeMapper.toResponse(room.getRoomType())
        );
    }

    public Room toEntity(RoomRequest request, RoomType roomType) {
        return Room.builder()
                .roomNumber(request.roomNumber())
                .status(request.status())
                .roomType(roomType)
                .build();
    }
}
