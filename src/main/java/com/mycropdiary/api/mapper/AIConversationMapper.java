package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.ai.AIConversationDetailRespDTO;
import com.mycropdiary.api.dto.ai.AIConversationRespDTO;
import com.mycropdiary.api.dto.ai.AIMessageRespDTO;
import com.mycropdiary.api.entity.AIConversation;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AIConversationMapper {

    public AIConversationRespDTO toResponse(AIConversation entity, long messageCount, String lastMessageSnippet) {
        if (entity == null) {
            return null;
        }
        return new AIConversationRespDTO(
                entity.getId(),
                entity.getConversationTitle(),
                entity.getConversationStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                messageCount,
                lastMessageSnippet
        );
    }

    public AIConversationDetailRespDTO toDetailResponse(AIConversation entity, List<AIMessageRespDTO> messages) {
        if (entity == null) {
            return null;
        }
        return new AIConversationDetailRespDTO(
                entity.getId(),
                entity.getConversationTitle(),
                entity.getConversationStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                messages != null ? messages : List.of()
        );
    }
}
