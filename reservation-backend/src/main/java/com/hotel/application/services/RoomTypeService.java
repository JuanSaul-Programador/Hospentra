package com.hotel.application.services;

import com.hotel.web.dto.RoomTypeRequest;
import com.hotel.web.dto.RoomTypeResponse;

import java.util.List;

public interface RoomTypeService {

    List<RoomTypeResponse> findAll();

    RoomTypeResponse findById(Long id);

    RoomTypeResponse create(RoomTypeRequest request);

    RoomTypeResponse update(Long id, RoomTypeRequest request);

    void delete(Long id);
}
