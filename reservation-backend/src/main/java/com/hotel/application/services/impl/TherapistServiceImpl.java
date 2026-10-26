package com.hotel.application.services.impl;

import com.hotel.application.services.TherapistService;
import com.hotel.domain.model.Therapist;
import com.hotel.domain.repository.TherapistRepository;
import com.hotel.domain.exception.ResourceNotFoundException;
import com.hotel.web.dto.TherapistRequest;
import com.hotel.web.dto.TherapistResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TherapistServiceImpl implements TherapistService {

    private final TherapistRepository therapistRepository;

    @Override
    public List<TherapistResponse> findAll() {
        return therapistRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<TherapistResponse> findActive() {
        return therapistRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public TherapistResponse findById(Long id) {
        Therapist therapist = therapistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found with id: " + id));
        return mapToResponse(therapist);
    }

    @Override
    @Transactional
    public TherapistResponse create(TherapistRequest request) {
        Therapist therapist = Therapist.builder()
                .name(request.name().trim())
                .specialty(request.specialty().trim())
                .phone(request.phone() != null ? request.phone().trim() : null)
                .active(request.active() != null ? request.active() : true)
                .build();
        Therapist saved = therapistRepository.save(therapist);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public TherapistResponse update(Long id, TherapistRequest request) {
        Therapist therapist = therapistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found with id: " + id));

        therapist.setName(request.name().trim());
        therapist.setSpecialty(request.specialty().trim());
        if (request.phone() != null) {
            therapist.setPhone(request.phone().trim());
        }
        if (request.active() != null) {
            therapist.setActive(request.active());
        }

        Therapist updated = therapistRepository.save(therapist);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public TherapistResponse toggleStatus(Long id) {
        Therapist therapist = therapistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Therapist not found with id: " + id));

        therapist.setActive(!Boolean.TRUE.equals(therapist.getActive()));
        Therapist updated = therapistRepository.save(therapist);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!therapistRepository.existsById(id)) {
            throw new ResourceNotFoundException("Therapist not found with id: " + id);
        }
        therapistRepository.deleteById(id);
    }

    private TherapistResponse mapToResponse(Therapist therapist) {
        return new TherapistResponse(
                therapist.getId(),
                therapist.getName(),
                therapist.getSpecialty(),
                therapist.getPhone(),
                therapist.getActive(),
                therapist.getCreatedAt()
        );
    }
}
