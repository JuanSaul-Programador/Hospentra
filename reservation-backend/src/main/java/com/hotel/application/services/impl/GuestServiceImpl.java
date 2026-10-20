package com.hotel.application.services.impl;

import com.hotel.application.mapper.GuestMapper;
import com.hotel.application.services.GuestService;
import com.hotel.domain.exception.ResourceNotFoundException;
import com.hotel.domain.model.Guest;
import com.hotel.domain.repository.GuestRepository;
import com.hotel.web.dto.GuestRequest;
import com.hotel.web.dto.GuestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuestServiceImpl implements GuestService {

    private final GuestRepository guestRepository;
    private final GuestMapper guestMapper;

    @Override
    public List<GuestResponse> findAll() {
        return guestRepository.findAll().stream()
                .map(guestMapper::toResponse)
                .toList();
    }

    @Override
    public GuestResponse findById(Long id) {
        return guestRepository.findById(id)
                .map(guestMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found with id: " + id));
    }

    @Override
    @Transactional
    public GuestResponse create(GuestRequest request) {
        if (guestRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("A guest with email '" + request.email() + "' already exists");
        }
        Guest guest = guestMapper.toEntity(request);
        return guestMapper.toResponse(guestRepository.save(guest));
    }

    @Override
    @Transactional
    public GuestResponse update(Long id, GuestRequest request) {
        Guest guest = guestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found with id: " + id));
        guest.setFirstName(request.firstName());
        guest.setLastName(request.lastName());
        guest.setEmail(request.email());
        guest.setPhone(request.phone());
        return guestMapper.toResponse(guestRepository.save(guest));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!guestRepository.existsById(id)) {
            throw new ResourceNotFoundException("Guest not found with id: " + id);
        }
        guestRepository.deleteById(id);
    }
}
