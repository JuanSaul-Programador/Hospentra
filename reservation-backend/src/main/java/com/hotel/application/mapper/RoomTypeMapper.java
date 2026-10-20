package com.hotel.application.mapper;

import com.hotel.domain.model.RoomType;
import com.hotel.web.dto.RoomTypeRequest;
import com.hotel.web.dto.RoomTypeResponse;
import org.springframework.stereotype.Component;

@Component
public class RoomTypeMapper {

    public RoomTypeResponse toResponse(RoomType roomType) {
        return new RoomTypeResponse(
                roomType.getId(),
                roomType.getName(),
                roomType.getDescription(),
                roomType.getCapacity(),
                roomType.getPricePerNight()
        );
    }

    public RoomType toEntity(RoomTypeRequest request) {
        return RoomType.builder()
                .name(request.name())
                .description(request.description())
                .capacity(request.capacity())
                .pricePerNight(request.pricePerNight())
                .build();
    }
}
