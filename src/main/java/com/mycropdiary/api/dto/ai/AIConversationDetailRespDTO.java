package com.mycropdiary.api.dto.ai;

import java.time.Instant;
import java.util.List;

public record AIConversationDetailRespDTO(
        Integer conversationId,
        String title,
        String status,
        Instant createdAt,
        Instant updatedAt,
        List<AIMessageRespDTO> messages
) {
}

