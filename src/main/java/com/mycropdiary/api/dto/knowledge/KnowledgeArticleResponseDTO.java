package com.mycropdiary.api.dto.knowledge;

import com.mycropdiary.api.entity.knowledge.KnowledgeStatus;
import java.time.LocalDateTime;

public record KnowledgeArticleResponseDTO(
        Long id,
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
