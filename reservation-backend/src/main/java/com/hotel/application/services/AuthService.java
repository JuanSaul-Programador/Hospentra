package com.hotel.application.services;

import com.hotel.web.dto.AuthResponse;
import com.hotel.web.dto.LoginRequest;
import com.hotel.web.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
