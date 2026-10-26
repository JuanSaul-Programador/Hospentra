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

    @Value("${security.admin.username:admin}")
    private String adminUsername;

    @Value("${security.admin.email:superadmin@hotel.com}")
    private String adminEmail;

    @Value("${security.admin.password:admin123}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername(adminUsername)) {
            User admin = User.builder()
                    .username(adminUsername)
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ROLE_ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
        }

        if (!userRepository.existsByUsername("cocina")) {
            User kitchenStation = User.builder()
                    .username("cocina")
                    .email("cocina@hospentra.com")
                    .password(passwordEncoder.encode("cocina123"))
                    .role(Role.ROLE_KITCHEN)
                    .enabled(true)
                    .build();
            userRepository.save(kitchenStation);
        }
    }
}
