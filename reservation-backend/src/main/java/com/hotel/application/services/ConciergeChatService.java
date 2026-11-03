package com.hotel.application.services;

import com.hotel.web.dto.ConciergeConversationSummary;
import com.hotel.web.dto.ConciergeMessageRequest;
import com.hotel.web.dto.ConciergeMessageResponse;
import com.hotel.web.dto.ConciergeStatusResponse;

import java.util.List;

public interface ConciergeChatService {
    ConciergeMessageResponse sendMessage(ConciergeMessageRequest request, String currentUsername);
    List<ConciergeMessageResponse> getMyConversation(String currentUsername);
    List<ConciergeMessageResponse> getConversationWithGuest(Long guestUserId, String staffUsername);
    List<ConciergeConversationSummary> getActiveConversations(String staffUsername);
    void markConversationAsRead(Long guestUserId, String currentUsername);
    long getUnreadCount(String currentUsername);
    ConciergeStatusResponse getConciergeStatus(String username);
    ConciergeConversationSummary claimConversation(Long guestUserId, String staffUsername);
}
