package com.hotel.application.services;

import com.hotel.web.dto.CreateStaffRequest;
import com.hotel.web.dto.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse createStaff(CreateStaffRequest request);

    List<UserResponse> findAll();
}
