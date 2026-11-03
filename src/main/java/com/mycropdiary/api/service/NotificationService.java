package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.notification.NotificationListResp;
import com.mycropdiary.api.dto.notification.NotificationResp;

public interface NotificationService {

    NotificationListResp getMyNotifications(Integer userId, int limit);

    NotificationResp markAsRead(Integer userId, Integer notificationId);

    void markAllAsRead(Integer userId);

    void createNotification(Integer userId, String title, String message, String type, String actionUrl);
}

