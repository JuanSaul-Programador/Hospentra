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
                roomType.getNameEn(),
                roomType.getDescription(),
                roomType.getDescriptionEn(),
                roomType.getCapacity(),
                roomType.getPricePerNight(),
                roomType.getCoverImageUrl()
        );
    }

    public RoomType toEntity(RoomTypeRequest request) {
        return RoomType.builder()
                .name(request.name())
                .nameEn(request.nameEn())
                .description(request.description())
                .descriptionEn(request.descriptionEn())
                .capacity(request.capacity())
                .pricePerNight(request.pricePerNight())
                .build();
    }
}
