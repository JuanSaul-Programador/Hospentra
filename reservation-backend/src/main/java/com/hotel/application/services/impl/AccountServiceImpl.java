package com.hotel.application.services.impl;

import com.hotel.application.services.AccountService;
import com.hotel.application.services.EmailService;
import com.hotel.domain.exception.ResourceNotFoundException;
import com.hotel.domain.model.Guest;
import com.hotel.domain.model.ReservationStatus;
import com.hotel.domain.model.Role;
import com.hotel.domain.model.User;
import com.hotel.domain.model.VerificationCode;
import com.hotel.domain.repository.GuestRepository;
import com.hotel.domain.repository.ReservationRepository;
import com.hotel.domain.repository.UserRepository;
import com.hotel.domain.repository.VerificationCodeRepository;
import com.hotel.web.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountServiceImpl implements AccountService {

    private final UserRepository userRepository;
    private final GuestRepository guestRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final ReservationRepository reservationRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserProfileResponse getProfile(String username) {
        User user = findUserByUsername(username);
        return mapToProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(String username, UpdateProfileRequest request) {
        User user = findUserByUsername(username);

        Guest guest = user.getGuest();
        if (guest == null) {
            guest = Guest.builder()
                    .firstName(request.firstName().trim())
                    .lastName(request.lastName().trim())
                    .email(user.getEmail())
                    .phone(request.phone() != null ? request.phone().trim() : null)
                    .build();
            guest = guestRepository.save(guest);
            user.setGuest(guest);
        } else {
            guest.setFirstName(request.firstName().trim());
            guest.setLastName(request.lastName().trim());
            if (request.phone() != null) {
                guest.setPhone(request.phone().trim());
            }
            guest = guestRepository.save(guest);
        }

        userRepository.save(user);
        return mapToProfileResponse(user);
    }

    @Override
    @Transactional
    public void requestPasswordChangeOtp(String username) {
        User user = findUserByUsername(username);

        List<VerificationCode> existingCodes = verificationCodeRepository.findActiveCodesByEmailAndType(
                user.getEmail(), "PASSWORD_RESET_OTP");
        for (VerificationCode c : existingCodes) {
            c.setUsed(true);
            verificationCodeRepository.save(c);
        }

        String otpCode = String.format("%06d", new SecureRandom().nextInt(1000000));
        VerificationCode vCode = VerificationCode.builder()
                .user(user)
                .code(otpCode)
                .type("PASSWORD_RESET_OTP")
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();
        verificationCodeRepository.save(vCode);

        String displayName = user.getGuest() != null ? user.getGuest().getFirstName() : user.getUsername();
        emailService.sendPasswordResetOtp(user.getEmail(), displayName, otpCode);
    }

    @Override
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = findUserByUsername(username);

        List<VerificationCode> activeCodes = verificationCodeRepository.findActiveCodesByEmailAndCodeAndType(
                user.getEmail(), request.code().trim(), "PASSWORD_RESET_OTP");

        if (activeCodes.isEmpty()) {
            int updated = verificationCodeRepository.incrementFailedAttemptsAndInvalidateIfExceeded(
                    user.getId(), "PASSWORD_RESET_OTP");
            if (updated > 0 && verificationCodeRepository.findActiveCodesByEmailAndType(user.getEmail(), "PASSWORD_RESET_OTP").isEmpty()) {
                throw new IllegalArgumentException("Demasiados intentos fallidos. Su código ha sido invalidado por seguridad. Solicite uno nuevo.");
            }
            throw new IllegalArgumentException("Código de verificación incorrecto o inexistente.");
        }

        VerificationCode validCode = activeCodes.get(0);
        if (validCode.isExpired()) {
            throw new IllegalArgumentException("El código de verificación ha expirado. Por favor solicite uno nuevo.");
        }

        String trimmedPassword = request.newPassword() != null ? request.newPassword().trim() : "";
        if (trimmedPassword.length() < 8 || !trimmedPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$")) {
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 8 caracteres e incluir mayúscula, minúscula y número.");
        }

        validCode.setUsed(true);
        verificationCodeRepository.save(validCode);

        user.setPassword(passwordEncoder.encode(trimmedPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public AccountDeletionStatusResponse requestAccountDeletion(String username, RequestAccountDeletionRequest request) {
        User user = findUserByUsername(username);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("La contraseña ingresada es incorrecta.");
        }

        if (user.isPendingDeletion()) {
            throw new IllegalStateException("Su cuenta ya se encuentra en proceso de eliminación.");
        }

        if (user.getRole() == Role.ROLE_ADMIN) {
            long activeAdmins = userRepository.countByRoleAndEnabledTrue(Role.ROLE_ADMIN);
            if (activeAdmins <= 1) {
                throw new IllegalStateException("No se puede solicitar la eliminación del único administrador activo del sistema.");
            }
        }

        if (user.getGuest() != null) {
            boolean hasActive = reservationRepository.existsByGuestIdAndStatusIn(
                    user.getGuest().getId(),
                    List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN)
            );
            if (hasActive) {
                throw new IllegalStateException("No puede solicitar la eliminación de su cuenta mientras tenga reservaciones activas o pendientes de check-in.");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledDate = now.plusDays(30);

        user.setDeletionRequestedAt(now);
        user.setScheduledDeletionDate(scheduledDate);
        userRepository.save(user);

        String displayName = user.getGuest() != null ? user.getGuest().getFirstName() : user.getUsername();
        String formattedDate = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(scheduledDate);
        emailService.sendAccountDeletionNotice(user.getEmail(), displayName, formattedDate);

        return new AccountDeletionStatusResponse(
                true,
                now,
                scheduledDate,
                "Solicitud de eliminación registrada. Su cuenta entrará en un periodo de gracia de 30 días."
        );
    }

    @Override
    @Transactional
    public AccountDeletionStatusResponse cancelAccountDeletion(String username) {
        User user = findUserByUsername(username);

        if (!user.isPendingDeletion()) {
            throw new IllegalStateException("Su cuenta no tiene ninguna solicitud de eliminación programada.");
        }

        user.setDeletionRequestedAt(null);
        user.setScheduledDeletionDate(null);
        userRepository.save(user);

        String displayName = user.getGuest() != null ? user.getGuest().getFirstName() : user.getUsername();
        emailService.sendAccountDeletionCancelled(user.getEmail(), displayName);

        return new AccountDeletionStatusResponse(
                false,
                null,
                null,
                "La solicitud de eliminación ha sido cancelada. Su cuenta se encuentra plenamente activa."
        );
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        String firstName = user.getGuest() != null ? user.getGuest().getFirstName() : "";
        String lastName = user.getGuest() != null ? user.getGuest().getLastName() : "";
        String phone = user.getGuest() != null ? user.getGuest().getPhone() : null;

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isEmailVerified(),
                firstName,
                lastName,
                phone,
                user.getDeletionRequestedAt(),
                user.getScheduledDeletionDate(),
                user.isPendingDeletion()
        );
    }
}
