package com.hotel.application.services.impl;

import com.hotel.application.services.UserService;
import com.hotel.domain.exception.ResourceNotFoundException;
import com.hotel.domain.model.Role;
import com.hotel.domain.model.Shift;
import com.hotel.domain.model.User;
import com.hotel.domain.model.WorkStatus;
import com.hotel.domain.repository.UserRepository;
import com.hotel.web.dto.CreateStaffRequest;
import com.hotel.web.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createStaff(CreateStaffRequest request) {
        if (request.role() == Role.ROLE_CUSTOMER) {
            throw new IllegalArgumentException("Staff accounts cannot have the CUSTOMER role");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username '" + request.username() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email '" + request.email() + "' is already in use");
        }

        Shift shift = request.shift() != null
                ? request.shift()
                : (request.role() == Role.ROLE_KITCHEN ? Shift.FLEXIBLE : Shift.MORNING);

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .enabled(true)
                .shift(shift)
                .workStatus(WorkStatus.ON_DUTY)
                .build();

        User savedUser = userRepository.save(user);

        return mapToResponse(savedUser);
    }

    @Override
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public UserResponse updateWorkStatus(Long userId, WorkStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setWorkStatus(status);
        if (status == WorkStatus.ON_BREAK) {
            user.setBreakStartedAt(LocalDateTime.now());
        } else if (status == WorkStatus.ON_DUTY || status == WorkStatus.OFF_DUTY) {
            user.setBreakStartedAt(null);
        }

        User updated = userRepository.save(user);
        return mapToResponse(updated);
    }

    @Override
    public UserResponse findById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return mapToResponse(user);
    }

    private UserResponse mapToResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getShift() != null ? user.getShift() : Shift.MORNING,
                user.getWorkStatus() != null ? user.getWorkStatus() : WorkStatus.ON_DUTY,
                user.getBreakStartedAt()
        );
    }
}
