package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.knowledge.KnowledgeArticleResponseDTO;
import com.mycropdiary.api.entity.knowledge.KnowledgeArticle;
import com.mycropdiary.api.entity.knowledge.KnowledgeStatus;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.mapper.KnowledgeArticleMapper;
import com.mycropdiary.api.repository.knowledge.KnowledgeArticleRepository;
import com.mycropdiary.api.service.KnowledgeArticleService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class KnowledgeArticleServiceImpl implements KnowledgeArticleService {

    private final KnowledgeArticleRepository knowledgeArticleRepository;
    private final KnowledgeArticleMapper knowledgeArticleMapper;

    public KnowledgeArticleServiceImpl(KnowledgeArticleRepository knowledgeArticleRepository,
                                       KnowledgeArticleMapper knowledgeArticleMapper) {
        this.knowledgeArticleRepository = knowledgeArticleRepository;
        this.knowledgeArticleMapper = knowledgeArticleMapper;
    }

    @Override
    public PageResponse<KnowledgeArticleResponseDTO> getPublicArticles(int page, int size, String keyword, String categoryId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt", "createdAt"));

        Specification<KnowledgeArticle> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Rule 1: status MUST be PUBLISHED or ACTIVE
            predicates.add(root.get("status").in(KnowledgeStatus.PUBLISHED, KnowledgeStatus.ACTIVE));

            // Rule 2: isPublic MUST be true (or null for backwards compatibility)
            Predicate isPublicTrue = criteriaBuilder.or(
                    criteriaBuilder.isNull(root.get("isPublic")),
                    criteriaBuilder.equal(root.get("isPublic"), true)
            );
            predicates.add(isPublicTrue);

            // Keyword filter (matches title OR summary)
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate titleLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern);
                Predicate summaryLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("summary")), pattern);
                predicates.add(criteriaBuilder.or(titleLike, summaryLike));
            }

            // Category filter
            if (StringUtils.hasText(categoryId)) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("category")), categoryId.trim().toLowerCase()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<KnowledgeArticleResponseDTO> dtoPage = knowledgeArticleRepository.findAll(spec, pageable)
                .map(knowledgeArticleMapper::toDTO);

        return PageResponse.from(dtoPage);
    }

    @Override
    public KnowledgeArticleResponseDTO getPublicArticleById(Long id) {
        KnowledgeArticle article = knowledgeArticleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge article not found with id: " + id));

        boolean isActiveStatus = article.getStatus() == KnowledgeStatus.PUBLISHED || article.getStatus() == KnowledgeStatus.ACTIVE;
        boolean isPublic = article.getIsPublic() == null || Boolean.TRUE.equals(article.getIsPublic());

        if (!isActiveStatus || !isPublic) {
            throw new ResourceNotFoundException("Knowledge article not found with id: " + id);
        }

        return knowledgeArticleMapper.toDTO(article);
    }
}
