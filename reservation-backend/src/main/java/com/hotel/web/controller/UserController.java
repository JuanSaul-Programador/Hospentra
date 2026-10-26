package com.hotel.web.controller;

import com.hotel.application.services.UserService;
import com.hotel.web.dto.CreateStaffRequest;
import com.hotel.web.dto.UpdateWorkStatusRequest;
import com.hotel.web.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createStaff(@Valid @RequestBody CreateStaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createStaff(request));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PatchMapping("/{id}/work-status")
    public ResponseEntity<UserResponse> updateWorkStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkStatusRequest request
    ) {
        return ResponseEntity.ok(userService.updateWorkStatus(id, request.workStatus()));
    }
}
