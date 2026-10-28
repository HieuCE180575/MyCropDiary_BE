package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.knowledge.KnowledgeArticleRespDTO;
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

/**
 * Lớp hiện thực hóa dịch vụ xử lý nghiệp vụ cho Bài viết Kiến thức Nông nghiệp (Public).
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

    @Override
    public PageResp<KnowledgeArticleRespDTO> getPublicArticles(int page, int size, String keyword, String categoryId) {
        return getPublicArticles(page, size, keyword, categoryId, "newest");
    }

    @Override
    public PageResp<KnowledgeArticleRespDTO> getPublicArticles(int page, int size, String keyword, String categoryId, String sort) {
        Sort sortOrder;
        if ("popular".equalsIgnoreCase(sort)) {
            sortOrder = Sort.by(Sort.Direction.DESC, "viewCount").and(Sort.by(Sort.Direction.DESC, "publishedAt"));
        } else {
            sortOrder = Sort.by(Sort.Direction.DESC, "publishedAt", "createdAt");
        }

        Pageable pageable = PageRequest.of(page, size, sortOrder);

        Specification<KnowledgeArticle> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Trạng thái bài viết BẮT BUỘC là published
            predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("articleStatus")), "published"));

            // 2. Lọc theo danh mục nếu được truyền và khác "all"
            if (StringUtils.hasText(categoryId) && !"all".equalsIgnoreCase(categoryId.trim())) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("category")),
                        categoryId.trim().toLowerCase()
                ));
            }

            // 3. Lọc theo từ khóa (tìm tương đối trong Tiêu đề, Tóm tắt hoặc Thẻ Tags)
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate titleLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern);
                Predicate summaryLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("summary")), pattern);
                Predicate tagsLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("tags")), pattern);
                predicates.add(criteriaBuilder.or(titleLike, summaryLike, tagsLike));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<KnowledgeArticleRespDTO> dtoPage = knowledgeArticleRepository.findAll(spec, pageable)
                .map(knowledgeArticleMapper::toDTO);

        return PageResp.from(dtoPage);
    }

    @Override
    @Transactional
    public KnowledgeArticleRespDTO getPublicArticleById(Integer id) {
        KnowledgeArticle article = knowledgeArticleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết kiến thức với ID: " + id));

        if (!"published".equalsIgnoreCase(article.getArticleStatus())) {
            throw new ResourceNotFoundException("Không tìm thấy bài viết kiến thức với ID: " + id);
        }

        article.incrementViewCount();
        knowledgeArticleRepository.save(article);

        return knowledgeArticleMapper.toDTO(article);
    }

    @Override
    @Transactional
    public KnowledgeArticleRespDTO getPublicArticleBySlug(String slug) {
        KnowledgeArticle article = knowledgeArticleRepository.findBySlugAndArticleStatusIgnoreCase(slug, "published")
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết kiến thức với đường dẫn: " + slug));

        article.incrementViewCount();
        knowledgeArticleRepository.save(article);

        return knowledgeArticleMapper.toDTO(article);
    }
}
