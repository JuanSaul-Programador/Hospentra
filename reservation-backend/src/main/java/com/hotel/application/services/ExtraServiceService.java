package com.hotel.application.services;

import com.hotel.domain.model.ExtraCategory;
import com.hotel.web.dto.ExtraServiceResponse;

import java.util.List;

public interface ExtraServiceService {
    List<ExtraServiceResponse> findAllAvailable();
    List<ExtraServiceResponse> findByCategory(ExtraCategory category);
    ExtraServiceResponse findById(Long id);
}
