package com.hotel.application.services;

import com.hotel.domain.model.ExtraCategory;
import com.hotel.domain.model.ExtraService;

import java.util.List;

public interface ExtraServiceService {
    List<ExtraService> findAllAvailable();
    List<ExtraService> findByCategory(ExtraCategory category);
    ExtraService findById(Long id);
}
