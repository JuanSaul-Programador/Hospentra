package com.hotel.domain.repository;

import com.hotel.domain.model.Role;
import com.hotel.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByGuestId(Long guestId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    boolean existsByEmailAndIdNot(String email, Long id);

    long countByRoleAndEnabledTrue(Role role);

    List<User> findByScheduledDeletionDateLessThanEqual(LocalDateTime dateTime);
}
