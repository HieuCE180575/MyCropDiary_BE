package com.mycropdiary.api.dto.notification;

import java.util.List;

public record NotificationListResp(
        List<NotificationResp> items,
        long unreadCount
) {
}

