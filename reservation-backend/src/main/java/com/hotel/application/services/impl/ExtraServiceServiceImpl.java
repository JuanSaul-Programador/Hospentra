package com.hotel.application.services.impl;

import com.hotel.application.services.ExtraServiceService;
import com.hotel.domain.exception.ResourceNotFoundException;
import com.hotel.domain.model.ExtraCategory;
import com.hotel.domain.model.ExtraService;
import com.hotel.domain.repository.ExtraServiceRepository;
import com.hotel.web.dto.ExtraServiceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExtraServiceServiceImpl implements ExtraServiceService {

    private final ExtraServiceRepository extraServiceRepository;

    @Override
    public List<ExtraServiceResponse> findAllAvailable() {
        return extraServiceRepository.findByAvailableTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<ExtraServiceResponse> findByCategory(ExtraCategory category) {
        return extraServiceRepository.findByCategoryAndAvailableTrue(category).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ExtraServiceResponse findById(Long id) {
        ExtraService service = extraServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Extra service not found with id: " + id));
        return toResponse(service);
    }

    private ExtraServiceResponse toResponse(ExtraService service) {
        return new ExtraServiceResponse(
                service.getId(),
                service.getName(),
                service.getNameEn(),
                service.getCategory(),
                service.getDescription(),
                service.getDescriptionEn(),
                service.getPrice(),
                service.getImageUrl(),
                service.isAvailable()
        );
    }
}
