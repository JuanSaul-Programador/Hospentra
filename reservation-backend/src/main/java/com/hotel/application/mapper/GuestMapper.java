package com.hotel.application.mapper;

import com.hotel.domain.model.Guest;
import com.hotel.web.dto.GuestRequest;
import com.hotel.web.dto.GuestResponse;
import org.springframework.stereotype.Component;

@Component
public class GuestMapper {

    public GuestResponse toResponse(Guest guest) {
        return new GuestResponse(
                guest.getId(),
                guest.getFirstName(),
                guest.getLastName(),
                guest.getEmail(),
                guest.getPhone()
        );
    }

    public Guest toEntity(GuestRequest request) {
        return Guest.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .phone(request.phone())
                .build();
    }
}
