package com.hotel.application.services.impl;

import com.hotel.application.services.AuthService;
import com.hotel.application.services.EmailService;
import com.hotel.domain.model.Guest;
import com.hotel.domain.model.Role;
import com.hotel.domain.model.User;
import com.hotel.domain.model.VerificationCode;
import com.hotel.domain.repository.GuestRepository;
import com.hotel.domain.repository.UserRepository;
import com.hotel.domain.repository.VerificationCodeRepository;
import com.hotel.infrastructure.security.jwt.JwtService;
import com.hotel.web.dto.AuthResponse;
import com.hotel.web.dto.LoginRequest;
import com.hotel.web.dto.RegisterRequest;
import com.hotel.web.dto.VerifyEmailRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final GuestRepository guestRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username '" + request.username() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email '" + request.email() + "' is already in use");
        }

        Guest guest = Guest.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .phone(request.phone())
                .build();
        guest = guestRepository.save(guest);

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.ROLE_CUSTOMER)
                .guest(guest)
                .enabled(false)
                .emailVerified(false)
                .build();

        User savedUser = userRepository.save(user);

        String otpCode = String.format("%06d", new SecureRandom().nextInt(1000000));
        VerificationCode vCode = VerificationCode.builder()
                .user(savedUser)
                .code(otpCode)
                .type("REGISTRATION_OTP")
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();
        verificationCodeRepository.save(vCode);

        emailService.sendVerificationOtp(savedUser.getEmail(), guest.getFirstName(), otpCode);

        return new AuthResponse(null, null, "Bearer", savedUser.getUsername(), savedUser.getEmail(), savedUser.getRole(), true);
    }

    @Override
    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        String email = request.email().trim().toLowerCase();
        String code = request.code().trim();

        List<VerificationCode> activeCodes = verificationCodeRepository.findActiveCodesByEmailAndCodeAndType(
                email, code, "REGISTRATION_OTP");

        if (activeCodes.isEmpty()) {
            User existingUser = userRepository.findByEmail(email).orElse(null);
            if (existingUser != null && existingUser.isEmailVerified() && existingUser.isEnabled()) {
                String token = jwtService.generateToken(existingUser);
                String refreshToken = jwtService.generateRefreshToken(existingUser);
                return new AuthResponse(token, refreshToken, "Bearer", existingUser.getUsername(), existingUser.getEmail(), existingUser.getRole(), false);
            }
            throw new IllegalArgumentException("Código de verificación incorrecto o inexistente.");
        }

        VerificationCode validCode = activeCodes.get(0);
        if (validCode.isExpired()) {
            throw new IllegalArgumentException("El código de verificación ha expirado. Por favor solicite uno nuevo.");
        }

        validCode.setUsed(true);
        verificationCodeRepository.save(validCode);

        User user = validCode.getUser();
        user.setEnabled(true);
        user.setEmailVerified(true);
        User verifiedUser = userRepository.save(user);

        String token = jwtService.generateToken(verifiedUser);
        String refreshToken = jwtService.generateRefreshToken(verifiedUser);

        return new AuthResponse(token, refreshToken, "Bearer", verifiedUser.getUsername(), verifiedUser.getEmail(), verifiedUser.getRole(), false);
    }

    @Override
    @Transactional
    public void resendVerificationCode(String email) {
        String cleanEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new IllegalArgumentException("No existe ninguna cuenta registrada con el correo: " + cleanEmail));

        if (user.isEmailVerified() && user.isEnabled()) {
            throw new IllegalArgumentException("Esta cuenta de correo ya se encuentra verificada. Puede iniciar sesión directamente.");
        }

        List<VerificationCode> activeCodes = verificationCodeRepository.findActiveCodesByEmailAndType(cleanEmail, "REGISTRATION_OTP");
        for (VerificationCode c : activeCodes) {
            c.setUsed(true);
            verificationCodeRepository.save(c);
        }

        String otpCode = String.format("%06d", new SecureRandom().nextInt(1000000));
        VerificationCode vCode = VerificationCode.builder()
                .user(user)
                .code(otpCode)
                .type("REGISTRATION_OTP")
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();
        verificationCodeRepository.save(vCode);

        String guestName = user.getGuest() != null ? user.getGuest().getFirstName() : user.getUsername();
        emailService.sendVerificationOtp(user.getEmail(), guestName, otpCode);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (DisabledException e) {
            throw new IllegalArgumentException("Su cuenta aún no ha sido verificada. Ingrese el código enviado a su correo para activarla.");
        }

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        String token = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(token, refreshToken, "Bearer", user.getUsername(), user.getEmail(), user.getRole(), false);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token cannot be empty");
        }
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        String username = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found for provided token"));

        if (!jwtService.isRefreshTokenValid(refreshToken, user)) {
            throw new IllegalArgumentException("Refresh token is expired or invalid");
        }

        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        return new AuthResponse(newAccessToken, newRefreshToken, "Bearer", user.getUsername(), user.getEmail(), user.getRole());
    }
}
