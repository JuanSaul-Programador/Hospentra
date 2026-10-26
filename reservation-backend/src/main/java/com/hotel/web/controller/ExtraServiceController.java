package com.hotel.web.controller;

import com.hotel.application.services.ExtraServiceService;
import com.hotel.domain.model.ExtraCategory;
import com.hotel.domain.model.ExtraService;
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
    public ResponseEntity<List<ExtraService>> findAllAvailable() {
        return ResponseEntity.ok(extraServiceService.findAllAvailable());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<ExtraService>> findByCategory(@PathVariable ExtraCategory category) {
        return ResponseEntity.ok(extraServiceService.findByCategory(category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExtraService> findById(@PathVariable Long id) {
        return ResponseEntity.ok(extraServiceService.findById(id));
    }
}
