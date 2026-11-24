package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.notification.NotificationListRespDTO;
import com.mycropdiary.api.dto.notification.NotificationRespDTO;

public interface NotificationService {

    NotificationListRespDTO getMyNotifications(Integer userId, int limit);

    NotificationRespDTO markAsRead(Integer userId, Integer notificationId);

    void markAllAsRead(Integer userId);

    void createNotification(Integer userId, String title, String message, String type, String actionUrl);
}

