package com.hotel.application.services;

import com.hotel.domain.model.RoomStatus;
import com.hotel.web.dto.HousekeepingUpdateRequest;
import com.hotel.web.dto.RoomRequest;
import com.hotel.web.dto.RoomResponse;

import java.util.List;

public interface RoomService {

    List<RoomResponse> findAll();

    List<RoomResponse> findByStatus(RoomStatus status);

    RoomResponse findById(Long id);

    RoomResponse create(RoomRequest request);

    RoomResponse update(Long id, RoomRequest request);

    RoomResponse updateStatus(Long id, RoomStatus status);

    RoomResponse updateHousekeeping(Long id, HousekeepingUpdateRequest request);

    void delete(Long id);
}
