package com.hotel.infrastructure.security.jwt;

import com.hotel.domain.model.Role;
import com.hotel.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String VALID_KEY = "gtGnR+VKBC/diGd0u0ELWGrKvqfDz6nvtLlj0fAv9uA2t97llvfHndtl8jHYSmPg";

    private JwtService jwtService;
    private User activeUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", VALID_KEY);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L);
        ReflectionTestUtils.setField(jwtService, "jwtRefreshExpiration", 604800000L);
        jwtService.validateSecretKey();

        activeUser = User.builder()
                .id(1L)
                .username("lucia")
                .email("lucia@hospentra.com")
                .password("encoded")
                .role(Role.ROLE_CUSTOMER)
                .enabled(true)
                .build();
    }

    @Test
    void validateSecretKey_WithValidKey_Succeeds() {
        jwtService.validateSecretKey();
    }

    @Test
    void validateSecretKey_WithShortKey_ThrowsIllegalStateException() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        ReflectionTestUtils.setField(jwtService, "secretKey", shortKey);

        assertThatThrownBy(() -> jwtService.validateSecretKey())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void validateSecretKey_WithBlankKey_ThrowsIllegalStateException() {
        ReflectionTestUtils.setField(jwtService, "secretKey", "   ");

        assertThatThrownBy(() -> jwtService.validateSecretKey())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be configured");
    }

    @Test
    void validateSecretKey_WithInvalidBase64_ThrowsIllegalStateException() {
        ReflectionTestUtils.setField(jwtService, "secretKey", "not-valid-base64!@#");

        assertThatThrownBy(() -> jwtService.validateSecretKey())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("valid Base64");
    }

    @Test
    void isTokenValid_WhenEnabledAndUnlocked_ReturnsTrue() {
        String token = jwtService.generateToken(activeUser);

        assertThat(jwtService.isTokenValid(token, activeUser)).isTrue();
    }

    @Test
    void isTokenValid_WhenUserDisabled_ReturnsFalse() {
        String token = jwtService.generateToken(activeUser);
        activeUser.setEnabled(false);

        assertThat(jwtService.isTokenValid(token, activeUser)).isFalse();
    }

    @Test
    void isTokenValid_WhenAccountLocked_ReturnsFalse() {
        String token = jwtService.generateToken(activeUser);
        UserDetails lockedUser = new UserDetails() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return List.of();
            }

            @Override
            public String getPassword() {
                return "encoded";
            }

            @Override
            public String getUsername() {
                return "lucia";
            }

            @Override
            public boolean isAccountNonExpired() {
                return true;
            }

            @Override
            public boolean isAccountNonLocked() {
                return false;
            }

            @Override
            public boolean isCredentialsNonExpired() {
                return true;
            }

            @Override
            public boolean isEnabled() {
                return true;
            }
        };

        assertThat(jwtService.isTokenValid(token, lockedUser)).isFalse();
    }

    @Test
    void isTokenValid_WhenTokenExpired_ReturnsFalse() {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);
        String expiredToken = jwtService.generateToken(activeUser);

        assertThat(jwtService.isTokenValid(expiredToken, activeUser)).isFalse();
    }

    @Test
    void isTokenValid_WhenDifferentUser_ReturnsFalse() {
        String token = jwtService.generateToken(activeUser);
        User otherUser = User.builder()
                .id(2L)
                .username("carlos")
                .role(Role.ROLE_CUSTOMER)
                .enabled(true)
                .build();

        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    void isRefreshTokenValid_WhenUserDisabled_ReturnsFalse() {
        String refreshToken = jwtService.generateRefreshToken(activeUser);
        activeUser.setEnabled(false);

        assertThat(jwtService.isRefreshTokenValid(refreshToken, activeUser)).isFalse();
    }
}
