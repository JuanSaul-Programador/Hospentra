package com.hotel.application.services;

import com.hotel.application.services.impl.AuthServiceImpl;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GuestRepository guestRepository;

    @Mock
    private VerificationCodeRepository verificationCodeRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthServiceImpl authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                guestRepository,
                verificationCodeRepository,
                emailService,
                passwordEncoder,
                jwtService,
                authenticationManager
        );

        sampleUser = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .password("encoded_pass")
                .role(Role.ROLE_CUSTOMER)
                .enabled(true)
                .emailVerified(true)
                .build();
    }

    @Test
    void register_ShouldReturnAuthResponseWithTokens() {
        RegisterRequest request = new RegisterRequest(
                "john_doe",
                "john@example.com",
                "Password123!",
                "John",
                "Doe",
                "+525512345678"
        );

        when(userRepository.existsByUsername("john_doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(guestRepository.save(any(Guest.class))).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(verificationCodeRepository.save(any(VerificationCode.class))).thenAnswer(i -> i.getArgument(0));

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.verificationRequired()).isTrue();
        assertThat(response.username()).isEqualTo("john_doe");
        assertThat(response.role()).isEqualTo(Role.ROLE_CUSTOMER);
        verify(emailService).sendVerificationOtp(eq("john@example.com"), eq("John"), anyString());
    }

    @Test
    void verifyEmail_ShouldActivateUserAndReturnTokens() {
        VerifyEmailRequest request = new VerifyEmailRequest("john@example.com", "123456");
        VerificationCode vCode = VerificationCode.builder()
                .id(1L)
                .user(sampleUser)
                .code("123456")
                .type("REGISTRATION_OTP")
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();

        when(verificationCodeRepository.findActiveCodesByEmailAndCodeAndType("john@example.com", "123456", "REGISTRATION_OTP"))
                .thenReturn(List.of(vCode));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("access_token_123");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh_token_456");

        AuthResponse response = authService.verifyEmail(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("access_token_123");
        assertThat(response.verificationRequired()).isFalse();
        assertThat(vCode.isUsed()).isTrue();
    }

    @Test
    void login_ShouldReturnAuthResponseWithTokens() {
        LoginRequest request = new LoginRequest("john_doe", "Password123!");

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateToken(sampleUser)).thenReturn("new_access_token");
        when(jwtService.generateRefreshToken(sampleUser)).thenReturn("new_refresh_token");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("new_access_token");
        assertThat(response.refreshToken()).isEqualTo("new_refresh_token");
        verify(authenticationManager, times(1)).authenticate(any());
    }

    @Test
    void refreshToken_ShouldSucceedWhenValid() {
        String oldRefreshToken = "valid_refresh_token";

        when(jwtService.isRefreshToken(oldRefreshToken)).thenReturn(true);
        when(jwtService.extractUsername(oldRefreshToken)).thenReturn("john_doe");
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(sampleUser));
        when(jwtService.isRefreshTokenValid(oldRefreshToken, sampleUser)).thenReturn(true);
        when(jwtService.generateToken(sampleUser)).thenReturn("fresh_access_token");
        when(jwtService.generateRefreshToken(sampleUser)).thenReturn("fresh_refresh_token");

        AuthResponse response = authService.refreshToken(oldRefreshToken);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("fresh_access_token");
        assertThat(response.refreshToken()).isEqualTo("fresh_refresh_token");
    }

    @Test
    void refreshToken_ShouldThrowWhenTokenNotRefreshType() {
        String invalidToken = "not_a_refresh_token";
        when(jwtService.isRefreshToken(invalidToken)).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken(invalidToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void refreshToken_ShouldThrowWhenExpired() {
        String expiredRefreshToken = "expired_refresh_token";

        when(jwtService.isRefreshToken(expiredRefreshToken)).thenReturn(true);
        when(jwtService.extractUsername(expiredRefreshToken)).thenReturn("john_doe");
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(sampleUser));
        when(jwtService.isRefreshTokenValid(expiredRefreshToken, sampleUser)).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken(expiredRefreshToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired or invalid");
    }

    @Test
    void refreshToken_ShouldThrowDisabledExceptionWhenUserIsDisabled() {
        String validRefreshToken = "valid_refresh_token";
        sampleUser.setEnabled(false);

        when(jwtService.isRefreshToken(validRefreshToken)).thenReturn(true);
        when(jwtService.extractUsername(validRefreshToken)).thenReturn("john_doe");
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> authService.refreshToken(validRefreshToken))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void verifyEmail_WhenInvalidCodeDoesNotExhaustAttempts_IncrementsDbCounter() {
        User pendingUser = User.builder()
                .id(7L)
                .username("pending_user")
                .email("pending@example.com")
                .enabled(false)
                .emailVerified(false)
                .build();
        VerifyEmailRequest request = new VerifyEmailRequest("pending@example.com", "000000");

        when(verificationCodeRepository.findActiveCodesByEmailAndCodeAndType("pending@example.com", "000000", "REGISTRATION_OTP"))
                .thenReturn(List.of());
        when(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(pendingUser));
        when(verificationCodeRepository.incrementFailedAttemptsAndInvalidateIfExceeded(7L, "REGISTRATION_OTP")).thenReturn(0);

        assertThatThrownBy(() -> authService.verifyEmail(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("incorrecto o inexistente");
        verify(verificationCodeRepository).incrementFailedAttemptsAndInvalidateIfExceeded(7L, "REGISTRATION_OTP");
    }

    @Test
    void verifyEmail_WhenMaxAttemptsExceeded_ThrowsLockoutMessage() {
        User pendingUser = User.builder()
                .id(7L)
                .username("pending_user")
                .email("pending@example.com")
                .enabled(false)
                .emailVerified(false)
                .build();
        VerifyEmailRequest request = new VerifyEmailRequest("pending@example.com", "000000");

        when(verificationCodeRepository.findActiveCodesByEmailAndCodeAndType("pending@example.com", "000000", "REGISTRATION_OTP"))
                .thenReturn(List.of());
        when(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(pendingUser));
        when(verificationCodeRepository.incrementFailedAttemptsAndInvalidateIfExceeded(7L, "REGISTRATION_OTP")).thenReturn(1);
        when(verificationCodeRepository.findActiveCodesByEmailAndType("pending@example.com", "REGISTRATION_OTP"))
                .thenReturn(List.of());

        assertThatThrownBy(() -> authService.verifyEmail(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Demasiados intentos fallidos");
    }
}
