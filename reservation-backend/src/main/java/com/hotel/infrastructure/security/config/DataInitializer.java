package com.hotel.infrastructure.security.config;

import com.hotel.domain.model.Role;
import com.hotel.domain.model.User;
import com.hotel.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${security.admin.username}")
    private String adminUsername;

    @Value("${security.admin.email}")
    private String adminEmail;

    @Value("${security.admin.password}")
    private String adminPassword;

    @Value("${security.kitchen.username}")
    private String kitchenUsername;

    @Value("${security.kitchen.email}")
    private String kitchenEmail;

    @Value("${security.kitchen.password}")
    private String kitchenPassword;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername(adminUsername)) {
            if (adminPassword != null && !adminPassword.isBlank()) {
                User admin = User.builder()
                        .username(adminUsername)
                        .email(adminEmail)
                        .password(passwordEncoder.encode(adminPassword))
                        .role(Role.ROLE_ADMIN)
                        .enabled(true)
                        .build();
                userRepository.save(admin);
            }
        }

        if (!userRepository.existsByUsername(kitchenUsername)) {
            if (kitchenPassword != null && !kitchenPassword.isBlank()) {
                User kitchenStation = User.builder()
                        .username(kitchenUsername)
                        .email(kitchenEmail)
                        .password(passwordEncoder.encode(kitchenPassword))
                        .role(Role.ROLE_KITCHEN)
                        .enabled(true)
                        .build();
                userRepository.save(kitchenStation);
            }
        }
    }
}
