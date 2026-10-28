package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.knowledge.KnowledgeArticleRespDTO;
import com.mycropdiary.api.entity.knowledge.KnowledgeArticle;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

@Component
public class KnowledgeArticleMapper {

    public KnowledgeArticleRespDTO toDTO(KnowledgeArticle entity) {
        if (entity == null) {
            return null;
        }

        List<String> tagList = parseTags(entity.getTags());
        int readTime = calculateReadTime(entity.getBody());

        Integer authorId = null;
        String authorName = null;
        String authorAvatar = null;
        if (entity.getCreatedBy() != null) {
            authorId = entity.getCreatedBy().getId();
            authorName = entity.getCreatedBy().getFullName();
            authorAvatar = entity.getCreatedBy().getAvatarUrl();
        }

        return new KnowledgeArticleRespDTO(
                entity.getId(),
                entity.getTitle(),
                entity.getSlug(),
                entity.getSummary(),
                entity.getContent(),
                entity.getCoverImageUrl(),
                entity.getCategory(),
                tagList,
                entity.getViewCount() != null ? entity.getViewCount() : 0,
                readTime,
                authorId,
                authorName,
                authorAvatar,
                entity.getSourceUrl(),
                entity.getStatus(),
                entity.getIsPublic(),
                toLocalDateTime(entity.getPublishedAt()),
                toLocalDateTime(entity.getCreatedAt()),
                toLocalDateTime(entity.getUpdatedAt())
        );
    }

    private List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split("[,;]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private int calculateReadTime(String body) {
        if (body == null || body.isBlank()) {
            return 1;
        }
        int words = body.trim().split("\\s+").length;
        return Math.max(1, (int) Math.ceil(words / 200.0));
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }
}
