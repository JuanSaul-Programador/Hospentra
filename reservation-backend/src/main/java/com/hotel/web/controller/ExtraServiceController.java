package com.hotel.web.controller;

import com.hotel.application.services.ExtraServiceService;
import com.hotel.domain.model.ExtraCategory;
import com.hotel.web.dto.ExtraServiceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/extra-services")
@RequiredArgsConstructor
public class ExtraServiceController {

    private final ExtraServiceService extraServiceService;

    @GetMapping
    public ResponseEntity<List<ExtraServiceResponse>> findAllAvailable() {
        return ResponseEntity.ok(extraServiceService.findAllAvailable());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<ExtraServiceResponse>> findByCategory(@PathVariable ExtraCategory category) {
        return ResponseEntity.ok(extraServiceService.findByCategory(category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExtraServiceResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(extraServiceService.findById(id));
    }
}
