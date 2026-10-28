package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.knowledge.CreateKnowledgeArticleReq;
import com.mycropdiary.api.dto.knowledge.KnowledgeArticleRespDTO;
import com.mycropdiary.api.dto.knowledge.UpdateKnowledgeArticleReq;
import com.mycropdiary.api.dto.knowledge.UpdateKnowledgeStatusReq;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.knowledge.KnowledgeArticle;
import com.mycropdiary.api.exception.ResourceConflictException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.mapper.KnowledgeArticleMapper;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.repository.knowledge.KnowledgeArticleRepository;
import com.mycropdiary.api.service.admin.AdminKnowledgeArticleService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Transactional
public class AdminKnowledgeArticleServiceImpl implements AdminKnowledgeArticleService {

    private final KnowledgeArticleRepository knowledgeArticleRepository;
    private final UserRepository userRepository;
    private final KnowledgeArticleMapper knowledgeArticleMapper;

    public AdminKnowledgeArticleServiceImpl(KnowledgeArticleRepository knowledgeArticleRepository,
                                           UserRepository userRepository,
                                           KnowledgeArticleMapper knowledgeArticleMapper) {
        this.knowledgeArticleRepository = knowledgeArticleRepository;
        this.userRepository = userRepository;
        this.knowledgeArticleMapper = knowledgeArticleMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResp<KnowledgeArticleRespDTO> getAdminArticles(String status, String category, String search, Pageable pageable) {
        Specification<KnowledgeArticle> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(status) && !"all".equalsIgnoreCase(status.trim())) {
                predicates.add(cb.equal(cb.lower(root.get("articleStatus")), status.trim().toLowerCase()));
            }

            if (StringUtils.hasText(category) && !"all".equalsIgnoreCase(category.trim())) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase()));
            }

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), pattern);
                Predicate summaryLike = cb.like(cb.lower(root.get("summary")), pattern);
                Predicate tagsLike = cb.like(cb.lower(root.get("tags")), pattern);
                predicates.add(cb.or(titleLike, summaryLike, tagsLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<KnowledgeArticleRespDTO> page = knowledgeArticleRepository.findAll(spec, pageable)
                .map(knowledgeArticleMapper::toDTO);

        return PageResp.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeArticleRespDTO getAdminArticleById(Integer id) {
        KnowledgeArticle article = knowledgeArticleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết kiến thức với ID: " + id));
        return knowledgeArticleMapper.toDTO(article);
    }

    @Override
    public KnowledgeArticleRespDTO createArticle(Integer adminUserId, CreateKnowledgeArticleReq request) {
        User author = null;
        if (adminUserId != null) {
            author = userRepository.findById(adminUserId).orElse(null);
        }

        String slug = generateUniqueSlug(request.title(), request.slug(), null);

        KnowledgeArticle article = new KnowledgeArticle();
        article.setCreatedBy(author);
        article.setTitle(request.title().trim());
        article.setSlug(slug);
        article.setSummary(request.summary() != null ? request.summary().trim() : null);
        article.setBody(request.body().trim());
        article.setCoverImageUrl(request.coverImageUrl() != null ? request.coverImageUrl().trim() : null);
        article.setCategory(request.category() != null ? request.category().trim().toLowerCase() : null);
        article.setTags(request.tags() != null ? request.tags().trim() : null);
        article.setSourceUrl(request.sourceUrl() != null ? request.sourceUrl().trim() : null);
        article.setViewCount(0);

        String status = StringUtils.hasText(request.status()) ? request.status().trim().toLowerCase() : "draft";
        article.setArticleStatus(status);
        if ("published".equalsIgnoreCase(status)) {
            article.setPublishedAt(Instant.now());
        }

        KnowledgeArticle saved = knowledgeArticleRepository.save(article);
        return knowledgeArticleMapper.toDTO(saved);
    }

    @Override
    public KnowledgeArticleRespDTO updateArticle(Integer adminUserId, Integer id, UpdateKnowledgeArticleReq request) {
        KnowledgeArticle article = knowledgeArticleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết kiến thức với ID: " + id));

        String slug = generateUniqueSlug(request.title(), request.slug(), id);

        article.setTitle(request.title().trim());
        article.setSlug(slug);
        article.setSummary(request.summary() != null ? request.summary().trim() : null);
        article.setBody(request.body().trim());
        article.setCoverImageUrl(request.coverImageUrl() != null ? request.coverImageUrl().trim() : null);
        article.setCategory(request.category() != null ? request.category().trim().toLowerCase() : null);
        article.setTags(request.tags() != null ? request.tags().trim() : null);
        article.setSourceUrl(request.sourceUrl() != null ? request.sourceUrl().trim() : null);

        if (StringUtils.hasText(request.status())) {
            String newStatus = request.status().trim().toLowerCase();
            if ("published".equalsIgnoreCase(newStatus) && article.getPublishedAt() == null) {
                article.setPublishedAt(Instant.now());
            }
            article.setArticleStatus(newStatus);
        }

        KnowledgeArticle saved = knowledgeArticleRepository.save(article);
        return knowledgeArticleMapper.toDTO(saved);
    }

    @Override
    public KnowledgeArticleRespDTO updateArticleStatus(Integer adminUserId, Integer id, UpdateKnowledgeStatusReq request) {
        KnowledgeArticle article = knowledgeArticleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết kiến thức với ID: " + id));

        String newStatus = request.status().trim().toLowerCase();
        if ("published".equalsIgnoreCase(newStatus) && article.getPublishedAt() == null) {
            article.setPublishedAt(Instant.now());
        }
        article.setArticleStatus(newStatus);

        KnowledgeArticle saved = knowledgeArticleRepository.save(article);
        return knowledgeArticleMapper.toDTO(saved);
    }

    @Override
    public void deleteArticle(Integer adminUserId, Integer id) {
        KnowledgeArticle article = knowledgeArticleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết kiến thức với ID: " + id));
        knowledgeArticleRepository.delete(article);
    }

    private String generateUniqueSlug(String title, String customSlug, Integer excludeId) {
        String base = StringUtils.hasText(customSlug) ? customSlug.trim() : title;
        String slug = toSlug(base);
        if (slug.isBlank()) {
            slug = "bai-viet-" + System.currentTimeMillis();
        }

        String finalSlug = slug;
        int counter = 1;
        while (isSlugExists(finalSlug, excludeId)) {
            finalSlug = slug + "-" + counter;
            counter++;
        }
        return finalSlug;
    }

    private boolean isSlugExists(String slug, Integer excludeId) {
        if (excludeId == null) {
            return knowledgeArticleRepository.existsBySlug(slug);
        }
        return knowledgeArticleRepository.existsBySlugAndIdNot(slug, excludeId);
    }

    private String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(input.trim().toLowerCase(), Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String noAccent = pattern.matcher(normalized).replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'd');
        String slug = noAccent.replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-").replaceAll("-+", "-");
        return slug.replaceAll("^-|-$", "");
    }
}

