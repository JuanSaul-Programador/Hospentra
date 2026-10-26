package com.hotel.application.services;

import com.hotel.web.dto.TherapistRequest;
import com.hotel.web.dto.TherapistResponse;

import java.util.List;

public interface TherapistService {
    List<TherapistResponse> findAll();
    List<TherapistResponse> findActive();
    TherapistResponse findById(Long id);
    TherapistResponse create(TherapistRequest request);
    TherapistResponse update(Long id, TherapistRequest request);
    TherapistResponse toggleStatus(Long id);
    void delete(Long id);
}
