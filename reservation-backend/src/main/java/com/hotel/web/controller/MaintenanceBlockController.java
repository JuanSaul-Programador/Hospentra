package com.hotel.web.controller;

import com.hotel.application.services.MaintenanceBlockService;
import com.hotel.web.dto.MaintenanceBlockRequest;
import com.hotel.web.dto.MaintenanceBlockResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/maintenance-blocks")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
public class MaintenanceBlockController {

    private final MaintenanceBlockService maintenanceBlockService;

    @PostMapping
    public ResponseEntity<MaintenanceBlockResponse> createBlock(@Valid @RequestBody MaintenanceBlockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(maintenanceBlockService.createBlock(request));
    }

    @GetMapping
    public ResponseEntity<List<MaintenanceBlockResponse>> getBlocks(@RequestParam(required = false) Long roomId) {
        if (roomId != null) {
            return ResponseEntity.ok(maintenanceBlockService.getBlocksByRoom(roomId));
        }
        return ResponseEntity.ok(maintenanceBlockService.getAllBlocks());
    }

    @GetMapping("/active")
    public ResponseEntity<List<MaintenanceBlockResponse>> getActiveBlocks(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(maintenanceBlockService.getActiveBlocks(startDate, endDate));
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<MaintenanceBlockResponse> resolveBlock(@PathVariable Long id) {
        return ResponseEntity.ok(maintenanceBlockService.resolveBlock(id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<MaintenanceBlockResponse> cancelBlock(@PathVariable Long id) {
        return ResponseEntity.ok(maintenanceBlockService.cancelBlock(id));
    }
}
