package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.knowledge.KnowledgeArticleRespDTO;
import com.mycropdiary.api.entity.knowledge.KnowledgeArticle;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class KnowledgeArticleMapper {

    public KnowledgeArticleRespDTO toDTO(KnowledgeArticle entity) {
        if (entity == null) {
            return null;
        }
        return new KnowledgeArticleRespDTO(
                entity.getId(),
                entity.getTitle(),
                entity.getSlug(),
                entity.getSummary(),
                entity.getContent(),
                entity.getCategory(),
                entity.getSourceUrl(),
                entity.getStatus(),
                entity.getIsPublic(),
                toLocalDateTime(entity.getPublishedAt()),
                toLocalDateTime(entity.getCreatedAt()),
                toLocalDateTime(entity.getUpdatedAt())
        );
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }
}
