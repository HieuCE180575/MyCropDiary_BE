package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.notification.NotificationRespDTO;
import com.mycropdiary.api.entity.Notification;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationMapper {

    public NotificationRespDTO toResponse(Notification entity) {
        return NotificationRespDTO.fromEntity(entity);
    }

    public List<NotificationRespDTO> toResponseList(List<Notification> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).toList();
    }
}
