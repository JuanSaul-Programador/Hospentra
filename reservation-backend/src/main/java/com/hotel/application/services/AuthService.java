package com.hotel.application.services;

import com.hotel.web.dto.AuthResponse;
import com.hotel.web.dto.LoginRequest;
import com.hotel.web.dto.RegisterRequest;
import com.hotel.web.dto.VerifyEmailRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse verifyEmail(VerifyEmailRequest request);

    void resendVerificationCode(String email);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshToken);
}
