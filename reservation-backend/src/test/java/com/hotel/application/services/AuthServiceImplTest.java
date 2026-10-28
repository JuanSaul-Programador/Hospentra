package com.hotel.application.services;

import com.hotel.application.services.impl.AuthServiceImpl;
import com.hotel.domain.model.Guest;
import com.hotel.domain.model.Role;
import com.hotel.domain.model.User;
import com.hotel.domain.repository.GuestRepository;
import com.hotel.domain.repository.UserRepository;
import com.hotel.infrastructure.security.jwt.JwtService;
import com.hotel.web.dto.AuthResponse;
import com.hotel.web.dto.LoginRequest;
import com.hotel.web.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

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
        when(jwtService.generateToken(sampleUser)).thenReturn("access_token_123");
        when(jwtService.generateRefreshToken(sampleUser)).thenReturn("refresh_token_456");

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("access_token_123");
        assertThat(response.refreshToken()).isEqualTo("refresh_token_456");
        assertThat(response.username()).isEqualTo("john_doe");
        assertThat(response.role()).isEqualTo(Role.ROLE_CUSTOMER);
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
}
