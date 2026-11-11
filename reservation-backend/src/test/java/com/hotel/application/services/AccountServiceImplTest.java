package com.hotel.application.services;

import com.hotel.application.services.impl.AccountServiceImpl;
import com.hotel.domain.model.Guest;
import com.hotel.domain.model.Role;
import com.hotel.domain.model.User;
import com.hotel.domain.model.VerificationCode;
import com.hotel.domain.repository.GuestRepository;
import com.hotel.domain.repository.ReservationRepository;
import com.hotel.domain.repository.UserRepository;
import com.hotel.domain.repository.VerificationCodeRepository;
import com.hotel.web.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GuestRepository guestRepository;

    @Mock
    private VerificationCodeRepository verificationCodeRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AccountServiceImpl accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountServiceImpl(
                userRepository,
                guestRepository,
                verificationCodeRepository,
                reservationRepository,
                emailService,
                passwordEncoder
        );
    }

    @Test
    void getProfile_Success() {
        Guest guest = Guest.builder().id(1L).firstName("Lucia").lastName("Perez").phone("5551234").build();
        User user = User.builder()
                .id(10L)
                .username("lucia")
                .email("lucia@hotel.com")
                .role(Role.ROLE_CUSTOMER)
                .emailVerified(true)
                .guest(guest)
                .build();
        when(userRepository.findByUsername("lucia")).thenReturn(Optional.of(user));

        UserProfileResponse response = accountService.getProfile("lucia");

        assertThat(response.username()).isEqualTo("lucia");
        assertThat(response.firstName()).isEqualTo("Lucia");
        assertThat(response.lastName()).isEqualTo("Perez");
        assertThat(response.pendingDeletion()).isFalse();
    }

    @Test
    void updateProfile_WhenGuestExists_UpdatesFields() {
        Guest guest = Guest.builder().id(1L).firstName("Lucia").lastName("Perez").phone("5551234").build();
        User user = User.builder().id(10L).username("lucia").guest(guest).build();
        when(userRepository.findByUsername("lucia")).thenReturn(Optional.of(user));
        when(guestRepository.save(any(Guest.class))).thenAnswer(i -> i.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest("Lucia Elena", "Perez Gomez", "5559999");
        UserProfileResponse response = accountService.updateProfile("lucia", request);

        assertThat(response.firstName()).isEqualTo("Lucia Elena");
        assertThat(response.lastName()).isEqualTo("Perez Gomez");
        assertThat(response.phone()).isEqualTo("5559999");
    }

    @Test
    void requestPasswordChangeOtp_Success() {
        User user = User.builder().id(10L).username("lucia").email("lucia@hotel.com").build();
        when(userRepository.findByUsername("lucia")).thenReturn(Optional.of(user));
        when(verificationCodeRepository.findActiveCodesByEmailAndType("lucia@hotel.com", "PASSWORD_RESET_OTP"))
                .thenReturn(Collections.emptyList());

        accountService.requestPasswordChangeOtp("lucia");

        verify(verificationCodeRepository).save(any(VerificationCode.class));
        verify(emailService).sendPasswordResetOtp(eq("lucia@hotel.com"), any(), any());
    }

    @Test
    void changePassword_Success() {
        User user = User.builder().id(10L).username("lucia").email("lucia@hotel.com").password("oldPass").build();
        when(userRepository.findByUsername("lucia")).thenReturn(Optional.of(user));

        VerificationCode code = VerificationCode.builder()
                .code("123456")
                .type("PASSWORD_RESET_OTP")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();
        when(verificationCodeRepository.findActiveCodesByEmailAndCodeAndType("lucia@hotel.com", "123456", "PASSWORD_RESET_OTP"))
                .thenReturn(List.of(code));
        when(passwordEncoder.encode("NewSecret123")).thenReturn("encodedNewSecret");

        ChangePasswordRequest request = new ChangePasswordRequest("123456", "NewSecret123");
        accountService.changePassword("lucia", request);

        assertThat(code.isUsed()).isTrue();
        assertThat(user.getPassword()).isEqualTo("encodedNewSecret");
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_WhenCodeInvalid_ThrowsIllegalArgumentException() {
        User user = User.builder().id(10L).username("lucia").email("lucia@hotel.com").build();
        when(userRepository.findByUsername("lucia")).thenReturn(Optional.of(user));
        when(verificationCodeRepository.findActiveCodesByEmailAndCodeAndType("lucia@hotel.com", "000000", "PASSWORD_RESET_OTP"))
                .thenReturn(Collections.emptyList());

        ChangePasswordRequest request = new ChangePasswordRequest("000000", "NewSecret123");

        assertThatThrownBy(() -> accountService.changePassword("lucia", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Código de verificación incorrecto o inexistente");
        verify(verificationCodeRepository).incrementFailedAttemptsAndInvalidateIfExceeded(10L, "PASSWORD_RESET_OTP");
    }

    @Test
    void changePassword_WhenOtpAttemptsExceeded_ThrowsLockoutMessage() {
        User user = User.builder().id(10L).username("lucia").email("lucia@hotel.com").build();
        when(userRepository.findByUsername("lucia")).thenReturn(Optional.of(user));
        when(verificationCodeRepository.findActiveCodesByEmailAndCodeAndType("lucia@hotel.com", "000000", "PASSWORD_RESET_OTP"))
                .thenReturn(Collections.emptyList());
        when(verificationCodeRepository.incrementFailedAttemptsAndInvalidateIfExceeded(10L, "PASSWORD_RESET_OTP"))
                .thenReturn(1);
        when(verificationCodeRepository.findActiveCodesByEmailAndType("lucia@hotel.com", "PASSWORD_RESET_OTP"))
                .thenReturn(Collections.emptyList());

        ChangePasswordRequest request = new ChangePasswordRequest("000000", "NewSecret123");

        assertThatThrownBy(() -> accountService.changePassword("lucia", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Demasiados intentos fallidos");
    }

    @Test
    void changePassword_WhenCodeExpired_ThrowsIllegalArgumentException() {
        User user = User.builder().id(10L).username("lucia").email("lucia@hotel.com").build();
        when(userRepository.findByUsername("lucia")).thenReturn(Optional.of(user));

        VerificationCode expiredCode = VerificationCode.builder()
                .code("123456")
                .type("PASSWORD_RESET_OTP")
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .used(false)
                .build();
        when(verificationCodeRepository.findActiveCodesByEmailAndCodeAndType("lucia@hotel.com", "123456", "PASSWORD_RESET_OTP"))
                .thenReturn(List.of(expiredCode));

        ChangePasswordRequest request = new ChangePasswordRequest("123456", "NewSecret123");

        assertThatThrownBy(() -> accountService.changePassword("lucia", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El código de verificación ha expirado");
    }

    @Test
    void changePassword_WhenPasswordShort_ThrowsIllegalArgumentException() {
        User user = User.builder().id(10L).username("lucia").email("lucia@hotel.com").build();
        when(userRepository.findByUsername("lucia")).thenReturn(Optional.of(user));

        VerificationCode code = VerificationCode.builder()
                .code("123456")
                .type("PASSWORD_RESET_OTP")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();
        when(verificationCodeRepository.findActiveCodesByEmailAndCodeAndType("lucia@hotel.com", "123456", "PASSWORD_RESET_OTP"))
                .thenReturn(List.of(code));

        ChangePasswordRequest request = new ChangePasswordRequest("123456", "123");

        assertThatThrownBy(() -> accountService.changePassword("lucia", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos 8 caracteres");
        assertThat(code.isUsed()).isFalse();
    }

    @Test
    void requestAccountDeletion_Success() {
        Guest guest = Guest.builder().id(1L).firstName("Lucia").build();
        User user = User.builder()
                .id(10L)
                .username("lucia")
                .email("lucia@hotel.com")
                .password("encodedSecret")
                .role(Role.ROLE_CUSTOMER)
                .guest(guest)
                .build();
        when(userRepository.findByUsername("lucia")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("myPassword", "encodedSecret")).thenReturn(true);
        when(reservationRepository.existsByGuestIdAndStatusIn(eq(1L), anyList())).thenReturn(false);

        RequestAccountDeletionRequest request = new RequestAccountDeletionRequest("myPassword", "Mudanza");
        AccountDeletionStatusResponse response = accountService.requestAccountDeletion("lucia", request);

        assertThat(response.pendingDeletion()).isTrue();
        assertThat(user.getScheduledDeletionDate()).isNotNull();
        assertThat(user.getDeletionRequestedAt()).isNotNull();
        verify(emailService).sendAccountDeletionNotice(eq("lucia@hotel.com"), eq("Lucia"), anyString());
    }

    @Test
    void requestAccountDeletion_WrongPassword_ThrowsIllegalArgumentException() {
        User user = User.builder().id(10L).username("lucia").password("encodedSecret").build();
        when(userRepository.findByUsername("lucia")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedSecret")).thenReturn(false);

        RequestAccountDeletionRequest request = new RequestAccountDeletionRequest("wrongPassword", "Mudanza");

        assertThatThrownBy(() -> accountService.requestAccountDeletion("lucia", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La contraseña ingresada es incorrecta");
    }

    @Test
    void requestAccountDeletion_WhenActiveReservations_ThrowsIllegalStateException() {
        Guest guest = Guest.builder().id(1L).build();
        User user = User.builder().id(10L).username("lucia").password("pass").guest(guest).build();
        when(userRepository.findByUsername("lucia")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "pass")).thenReturn(true);
        when(reservationRepository.existsByGuestIdAndStatusIn(eq(1L), anyList())).thenReturn(true);

        RequestAccountDeletionRequest request = new RequestAccountDeletionRequest("pass", "Mudanza");

        assertThatThrownBy(() -> accountService.requestAccountDeletion("lucia", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mientras tenga reservaciones activas");
    }

    @Test
    void requestAccountDeletion_WhenLastAdmin_ThrowsIllegalStateException() {
        User admin = User.builder().id(1L).username("admin").password("pass").role(Role.ROLE_ADMIN).build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("pass", "pass")).thenReturn(true);
        when(userRepository.countByRoleAndEnabledTrue(Role.ROLE_ADMIN)).thenReturn(1L);

        RequestAccountDeletionRequest request = new RequestAccountDeletionRequest("pass", "Cierre");

        assertThatThrownBy(() -> accountService.requestAccountDeletion("admin", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("del único administrador activo");
    }

    @Test
    void cancelAccountDeletion_Success() {
        User user = User.builder()
                .id(10L)
                .username("lucia")
                .email("lucia@hotel.com")
                .deletionRequestedAt(LocalDateTime.now().minusDays(5))
                .scheduledDeletionDate(LocalDateTime.now().plusDays(25))
                .build();
        when(userRepository.findByUsername("lucia")).thenReturn(Optional.of(user));

        AccountDeletionStatusResponse response = accountService.cancelAccountDeletion("lucia");

        assertThat(response.pendingDeletion()).isFalse();
        assertThat(user.getScheduledDeletionDate()).isNull();
        assertThat(user.getDeletionRequestedAt()).isNull();
        verify(emailService).sendAccountDeletionCancelled(eq("lucia@hotel.com"), anyString());
    }

    @Test
    void cancelAccountDeletion_WhenNotPending_ThrowsIllegalStateException() {
        User user = User.builder().id(10L).username("lucia").scheduledDeletionDate(null).build();
        when(userRepository.findByUsername("lucia")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> accountService.cancelAccountDeletion("lucia"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no tiene ninguna solicitud de eliminación programada");
    }
}
