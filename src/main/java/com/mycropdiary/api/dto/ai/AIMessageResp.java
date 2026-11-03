package com.mycropdiary.api.dto.ai;

import com.mycropdiary.api.entity.AIMessage;
import java.time.Instant;
import java.util.List;

public record AIMessageResp(
        Integer messageId,
        Integer conversationId,
        String senderType,
        String messageText,
        String modelName,
        Instant createdAt,
        List<String> sources
) {
    public static AIMessageResp fromEntity(AIMessage entity, List<String> sources) {
        if (entity == null) return null;
        return new AIMessageResp(
                entity.getId(),
                entity.getConversation().getId(),
                entity.getSenderType(),
                entity.getMessageText(),
                entity.getModelName(),
                entity.getCreatedAt(),
                sources != null ? sources : List.of()
        );
    }
}

