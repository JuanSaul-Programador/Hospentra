package com.hotel.application.services;

import com.hotel.domain.model.User;
import com.hotel.web.dto.NotificationResponse;

import java.util.List;

public interface InAppNotificationService {

    List<NotificationResponse> getMyNotifications(int limit);

    long getUnreadCount();

    NotificationResponse markAsRead(Long id);

    void markAllAsRead();

    NotificationResponse createNotification(User user, String title, String message, String type, String link);

    NotificationResponse createNotificationForUsername(String username, String title, String message, String type, String link);
}
