package com.hotel.application.services.impl;

import com.hotel.application.mapper.RoomTypeMapper;
import com.hotel.application.services.RoomTypeService;
import com.hotel.domain.exception.ResourceNotFoundException;
import com.hotel.domain.model.RoomType;
import com.hotel.domain.repository.RoomTypeRepository;
import com.hotel.web.dto.RoomTypeRequest;
import com.hotel.web.dto.RoomTypeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomTypeServiceImpl implements RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final RoomTypeMapper roomTypeMapper;

    @Override
    public List<RoomTypeResponse> findAll() {
        return roomTypeRepository.findAll().stream()
                .map(roomTypeMapper::toResponse)
                .toList();
    }

    @Override
    public RoomTypeResponse findById(Long id) {
        return roomTypeRepository.findById(id)
                .map(roomTypeMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("RoomType not found with id: " + id));
    }

    @Override
    @Transactional
    public RoomTypeResponse create(RoomTypeRequest request) {
        if (roomTypeRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("A room type with name '" + request.name() + "' already exists");
        }
        RoomType roomType = roomTypeMapper.toEntity(request);
        return roomTypeMapper.toResponse(roomTypeRepository.save(roomType));
    }

    @Override
    @Transactional
    public RoomTypeResponse update(Long id, RoomTypeRequest request) {
        RoomType roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RoomType not found with id: " + id));
        roomType.setName(request.name());
        roomType.setNameEn(request.nameEn());
        roomType.setDescription(request.description());
        roomType.setDescriptionEn(request.descriptionEn());
        roomType.setCapacity(request.capacity());
        roomType.setPricePerNight(request.pricePerNight());
        return roomTypeMapper.toResponse(roomTypeRepository.save(roomType));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!roomTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("RoomType not found with id: " + id);
        }
        roomTypeRepository.deleteById(id);
    }
}
