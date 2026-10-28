package com.mycropdiary.api.dto.knowledge;

import com.mycropdiary.api.entity.knowledge.KnowledgeStatus;
import java.time.LocalDateTime;
import java.util.List;

public record KnowledgeArticleRespDTO(
        Integer id,
        String title,
        String slug,
        String summary,
        String content,
        String coverImageUrl,
        String category,
        List<String> tags,
        Integer viewCount,
        Integer readTimeMinutes,
        Integer authorId,
        String authorName,
        String authorAvatar,
        String sourceUrl,
        KnowledgeStatus status,
        Boolean isPublic,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * Constructor tương thích ngược cho các unit test và API cũ (12 tham số).
     */
    public KnowledgeArticleRespDTO(
            Integer id,
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
        this(
                id,
                title,
                slug,
                summary,
                content,
                null,
                category,
                List.of(),
                0,
                1,
                null,
                null,
                null,
                sourceUrl,
                status,
                isPublic,
                publishedAt,
                createdAt,
                updatedAt
        );
    }
}
