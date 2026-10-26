package com.hotel.web.controller;

import com.hotel.application.services.TherapistService;
import com.hotel.web.dto.TherapistRequest;
import com.hotel.web.dto.TherapistResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/therapists")
@RequiredArgsConstructor
public class TherapistController {

    private final TherapistService therapistService;

    @GetMapping
    public ResponseEntity<List<TherapistResponse>> findAll() {
        return ResponseEntity.ok(therapistService.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<TherapistResponse>> findActive() {
        return ResponseEntity.ok(therapistService.findActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TherapistResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(therapistService.findById(id));
    }

    @PostMapping
    public ResponseEntity<TherapistResponse> create(@Valid @RequestBody TherapistRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(therapistService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TherapistResponse> update(@PathVariable Long id, @Valid @RequestBody TherapistRequest request) {
        return ResponseEntity.ok(therapistService.update(id, request));
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<TherapistResponse> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(therapistService.toggleStatus(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        therapistService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
