package com.hotel.domain.repository;

import com.hotel.domain.model.Guest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestRepository extends JpaRepository<Guest, Long> {

    boolean existsByEmail(String email);
}
