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
        // Cấu hình phân trang và sắp xếp giảm dần theo ngày xuất bản/ngày tạo
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "publishedAt", "createdAt"));

        // Xây dựng điều kiện truy vấn động bằng Specification
        Specification<KnowledgeArticle> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Điều kiện 1: Trạng thái bài viết BẮT BUỘC là PUBLISHED hoặc ACTIVE
            predicates.add(root.get("status").in(KnowledgeStatus.PUBLISHED, KnowledgeStatus.ACTIVE));

            // Điều kiện 2: Quyền công khai isPublic phải là true (hoặc null để tương thích dữ liệu cũ)
            Predicate isPublicTrue = criteriaBuilder.or(
                    criteriaBuilder.isNull(root.get("isPublic")),
                    criteriaBuilder.equal(root.get("isPublic"), true)
            );
            predicates.add(isPublicTrue);

            // Điều kiện 3: Lọc theo từ khóa (tìm tương đối trong Tiêu đề HOẶC Tóm tắt)
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate titleLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern);
                Predicate summaryLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("summary")), pattern);
                predicates.add(criteriaBuilder.or(titleLike, summaryLike));
            }

            // Điều kiện 4: Lọc chính xác theo danh mục (Category)
            if (StringUtils.hasText(categoryId)) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("category")), categoryId.trim().toLowerCase()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        // Thực thi truy vấn DB và ánh xạ từ Entity sang DTO
        Page<KnowledgeArticleResponseDTO> dtoPage = knowledgeArticleRepository.findAll(spec, pageable)
                .map(knowledgeArticleMapper::toDTO);

        return PageResponse.from(dtoPage);
    }

    /**
     * UC-02: Lấy thông tin chi tiết một bài viết công khai theo ID.
     */
    @Override
    public KnowledgeArticleResponseDTO getPublicArticleById(Long id) {
        // Tìm bài viết theo ID trong DB, nếu không có ném ngoại lệ 404 Not Found
        KnowledgeArticle article = knowledgeArticleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài viết kiến thức với ID: " + id));

        // Kiểm tra bài viết có hợp lệ để xem công khai hay không
        boolean isActiveStatus = article.getStatus() == KnowledgeStatus.PUBLISHED || article.getStatus() == KnowledgeStatus.ACTIVE;
        boolean isPublic = article.getIsPublic() == null || Boolean.TRUE.equals(article.getIsPublic());

        // Nếu bài viết đang là Bản nháp (DRAFT) hoặc bị Ẩn (isPublic = false) -> Ném ngoại lệ 404 bảo mật
        if (!isActiveStatus || !isPublic) {
            throw new ResourceNotFoundException("Không tìm thấy bài viết kiến thức với ID: " + id);
        }

        return knowledgeArticleMapper.toDTO(article);
    }
}
