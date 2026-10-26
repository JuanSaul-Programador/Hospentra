package com.hotel.application.services.impl;

import com.hotel.application.services.ExtraServiceService;
import com.hotel.domain.exception.ResourceNotFoundException;
import com.hotel.domain.model.ExtraCategory;
import com.hotel.domain.model.ExtraService;
import com.hotel.domain.repository.ExtraServiceRepository;
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
    public List<ExtraService> findAllAvailable() {
        return extraServiceRepository.findByAvailableTrue();
    }

    @Override
    public List<ExtraService> findByCategory(ExtraCategory category) {
        return extraServiceRepository.findByCategoryAndAvailableTrue(category);
    }

    @Override
    public ExtraService findById(Long id) {
        return extraServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Extra service not found with id: " + id));
    }
}
