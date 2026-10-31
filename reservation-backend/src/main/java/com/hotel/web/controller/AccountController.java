package com.hotel.web.controller;

import com.hotel.application.services.AccountService;
import com.hotel.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Principal principal) {
        String username = principal != null ? principal.getName() : null;
        return ResponseEntity.ok(accountService.getProfile(username));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : null;
        return ResponseEntity.ok(accountService.updateProfile(username, request));
    }

    @PostMapping("/change-password/request-otp")
    public ResponseEntity<Void> requestPasswordChangeOtp(Principal principal) {
        String username = principal != null ? principal.getName() : null;
        accountService.requestPasswordChangeOtp(username);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : null;
        accountService.changePassword(username, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/request-deletion")
    public ResponseEntity<AccountDeletionStatusResponse> requestAccountDeletion(
            @Valid @RequestBody RequestAccountDeletionRequest request,
            Principal principal
    ) {
        String username = principal != null ? principal.getName() : null;
        return ResponseEntity.ok(accountService.requestAccountDeletion(username, request));
    }

    @PostMapping("/cancel-deletion")
    public ResponseEntity<AccountDeletionStatusResponse> cancelAccountDeletion(Principal principal) {
        String username = principal != null ? principal.getName() : null;
        return ResponseEntity.ok(accountService.cancelAccountDeletion(username));
    }
}
