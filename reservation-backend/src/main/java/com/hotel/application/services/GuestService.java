package com.hotel.application.services;

import com.hotel.web.dto.GuestRequest;
import com.hotel.web.dto.GuestResponse;

import java.util.List;

public interface GuestService {

    List<GuestResponse> findAll();

    GuestResponse findById(Long id);

    GuestResponse create(GuestRequest request);

    GuestResponse update(Long id, GuestRequest request);

    void delete(Long id);
}
