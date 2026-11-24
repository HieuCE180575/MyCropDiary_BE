package com.mycropdiary.api.dto.notification;

import com.mycropdiary.api.entity.Notification;
import java.time.Instant;

public record NotificationRespDTO(
        Integer id,
        String title,
        String message,
        String notificationType,
        Boolean isRead,
        String actionUrl,
        Instant createdAt
) {
    public static NotificationRespDTO fromEntity(Notification entity) {
        if (entity == null) return null;
        return new NotificationRespDTO(
                entity.getId(),
                entity.getTitle(),
                entity.getMessage(),
                entity.getNotificationType(),
                entity.getIsRead(),
                entity.getActionUrl(),
                entity.getCreatedAt()
        );
    }
}

