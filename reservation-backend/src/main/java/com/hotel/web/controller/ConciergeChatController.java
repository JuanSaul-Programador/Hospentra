package com.hotel.web.controller;

import com.hotel.application.services.ConciergeChatService;
import com.hotel.web.dto.ConciergeConversationSummary;
import com.hotel.web.dto.ConciergeMessageRequest;
import com.hotel.web.dto.ConciergeMessageResponse;
import com.hotel.web.dto.ConciergeStatusResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/concierge")
@RequiredArgsConstructor
public class ConciergeChatController {

    private final ConciergeChatService conciergeChatService;

    @GetMapping("/status")
    public ResponseEntity<ConciergeStatusResponse> getStatus(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        if (username == null) {
            return ResponseEntity.ok(new ConciergeStatusResponse(false, null, null, null));
        }
        return ResponseEntity.ok(conciergeChatService.getConciergeStatus(username));
    }

    @PostMapping("/messages")
    public ResponseEntity<ConciergeMessageResponse> sendMessage(
            @RequestBody @Valid ConciergeMessageRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        ConciergeMessageResponse response = conciergeChatService.sendMessage(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-messages")
    public ResponseEntity<List<ConciergeMessageResponse>> getMyConversation(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(conciergeChatService.getMyConversation(username));
    }

    @GetMapping("/conversations")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<List<ConciergeConversationSummary>> getActiveConversations(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(conciergeChatService.getActiveConversations(username));
    }

    @GetMapping("/conversations/{guestUserId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<List<ConciergeMessageResponse>> getConversationWithGuest(
            @PathVariable Long guestUserId,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(conciergeChatService.getConversationWithGuest(guestUserId, username));
    }

    @PatchMapping("/conversations/{guestUserId}/claim")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<ConciergeConversationSummary> claimConversation(
            @PathVariable Long guestUserId,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(conciergeChatService.claimConversation(guestUserId, username));
    }

    @PatchMapping("/conversations/{guestUserId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long guestUserId,
            Authentication authentication) {
        String username = authentication.getName();
        conciergeChatService.markConversationAsRead(guestUserId, username);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        String username = authentication.getName();
        long count = conciergeChatService.getUnreadCount(username);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }
}
