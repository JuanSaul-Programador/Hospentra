package com.hotel.domain.repository;

import com.hotel.domain.model.ExtraCategory;
import com.hotel.domain.model.ExtraService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExtraServiceRepository extends JpaRepository<ExtraService, Long> {
    List<ExtraService> findByAvailableTrue();
    List<ExtraService> findByCategoryAndAvailableTrue(ExtraCategory category);
}
