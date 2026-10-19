package com.mycropdiary.api.dto.knowledge;

import com.mycropdiary.api.entity.knowledge.KnowledgeStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record KnowledgeArticleResponseDTO(
        UUID id,
        String title,
        String slug,
        String summary,
        String content,
        String category,
        String sourceUrl,
        KnowledgeStatus status,
        Boolean isPublic,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
