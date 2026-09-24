package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.knowledge.KnowledgeArticleResponseDTO;
import com.mycropdiary.api.entity.knowledge.KnowledgeArticle;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeArticleMapper {

    public KnowledgeArticleResponseDTO toDTO(KnowledgeArticle entity) {
        if (entity == null) {
            return null;
        }
        return new KnowledgeArticleResponseDTO(
                entity.getId(),
                entity.getTitle(),
                entity.getSlug(),
                entity.getSummary(),
                entity.getContent(),
                entity.getCategory(),
                entity.getSourceUrl(),
                entity.getStatus(),
                entity.getIsPublic(),
                entity.getPublishedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
