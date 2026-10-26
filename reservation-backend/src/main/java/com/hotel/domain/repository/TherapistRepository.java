package com.hotel.domain.repository;

import com.hotel.domain.model.Therapist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TherapistRepository extends JpaRepository<Therapist, Long> {
    List<Therapist> findAllByOrderByCreatedAtDesc();
    List<Therapist> findByActiveTrueOrderByNameAsc();
}
