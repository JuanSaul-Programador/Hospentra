package com.hotel.application.services;

import com.hotel.web.dto.MaintenanceBlockRequest;
import com.hotel.web.dto.MaintenanceBlockResponse;

import java.time.LocalDate;
import java.util.List;

public interface MaintenanceBlockService {

    MaintenanceBlockResponse createBlock(MaintenanceBlockRequest request);

    MaintenanceBlockResponse resolveBlock(Long id);

    MaintenanceBlockResponse cancelBlock(Long id);

    List<MaintenanceBlockResponse> getActiveBlocks(LocalDate startDate, LocalDate endDate);

    List<MaintenanceBlockResponse> getAllBlocks();

    List<MaintenanceBlockResponse> getBlocksByRoom(Long roomId);
}
