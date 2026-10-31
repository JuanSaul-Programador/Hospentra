package com.hotel.application.services;

import com.hotel.domain.model.WorkStatus;
import com.hotel.web.dto.CreateStaffRequest;
import com.hotel.web.dto.UpdateStaffRequest;
import com.hotel.web.dto.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse createStaff(CreateStaffRequest request);

    List<UserResponse> findAll();

    UserResponse updateWorkStatus(Long userId, WorkStatus status);

    UserResponse findById(Long userId);

    UserResponse updateStaff(Long userId, UpdateStaffRequest request, String currentUsername);

    UserResponse toggleStaffStatus(Long userId, String currentUsername);
}
