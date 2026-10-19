package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.knowledge.KnowledgeArticleResponseDTO;
import com.mycropdiary.api.entity.knowledge.KnowledgeArticle;
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
import java.util.UUID;

/**
 * Lớp hiện thực hóa dịch vụ xử lý nghiệp vụ cho Bài viết Kiến thức Nông nghiệp.
 */
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

    /**
     * UC-01: Lấy danh sách các bài viết công khai có phân trang, lọc và tìm kiếm.
     */
    @Override
    public PageResponse<KnowledgeArticleResponseDTO> getPublicArticles(int page, int size, String keyword, String categoryId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt", "createdAt"));

        Specification<KnowledgeArticle> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Trạng thái bài viết BẮT BUỘC là published
            predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("articleStatus")), "published"));

            // Lọc theo từ khóa (tìm tương đối trong Tiêu đề HOẶC Tóm tắt)
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate titleLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern);
                Predicate summaryLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("summary")), pattern);
                predicates.add(criteriaBuilder.or(titleLike, summaryLike));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<KnowledgeArticleResponseDTO> dtoPage = knowledgeArticleRepository.findAll(spec, pageable)
                .map(knowledgeArticleMapper::toDTO);

        return PageResponse.from(dtoPage);
    }

    /**
     * UC-02: Lấy thông tin chi tiết một bài viết công khai theo ID.
     */
    @Override
    public KnowledgeArticleResponseDTO getPublicArticleById(UUID id) {
        KnowledgeArticle article = knowledgeArticleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết kiến thức với ID: " + id));

        if (!"published".equalsIgnoreCase(article.getArticleStatus())) {
            throw new ResourceNotFoundException("Không tìm thấy bài viết kiến thức với ID: " + id);
        }

        return knowledgeArticleMapper.toDTO(article);
    }
}
