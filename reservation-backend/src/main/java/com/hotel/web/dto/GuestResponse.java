package com.hotel.web.dto;

public record GuestResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone
) {}
