package com.mycropdiary.api.dto.notification;

import java.util.List;

public record NotificationListRespDTO(
        List<NotificationRespDTO> items,
        long unreadCount
) {
}

