package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.knowledge.KnowledgeArticleRespDTO;
import com.mycropdiary.api.service.KnowledgeArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý các API công khai liên quan đến Cẩm nang Kiến thức Nông nghiệp (Knowledge Articles).
 * Cho phép khách và người dùng xem danh sách, tìm kiếm, lọc theo chuyên mục và xem chi tiết bài viết.
 */
@Tag(name = "UC-01 & UC-02: Cẩm nang Kiến thức Nông nghiệp (Public)", description = "APIs công khai xem danh sách và chi tiết bài viết kiến thức nông nghiệp")
@RestController
@RequestMapping("/api/v1/knowledge/articles")
public class KnowledgeArticleController {

    private final KnowledgeArticleService knowledgeArticleService;

    public KnowledgeArticleController(KnowledgeArticleService knowledgeArticleService) {
        this.knowledgeArticleService = knowledgeArticleService;
    }

    /**
     * UC-01: Lấy danh sách các bài viết kiến thức công khai (Có phân trang, tìm kiếm, lọc theo danh mục và sắp xếp).
     */
    @Operation(summary = "UC-01: Lấy danh sách bài viết kiến thức công khai",
            description = "Hỗ trợ phân trang, tìm kiếm từ khóa trong tiêu đề/tóm tắt/tags, lọc theo chuyên mục (category) và sắp xếp theo mới nhất (newest) hoặc phổ biến nhất (popular).")
    @GetMapping
    public ApiResp<PageResp<KnowledgeArticleRespDTO>> getPublicArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "newest") String sort
    ) {
        String effectiveCategory = categoryId != null ? categoryId : category;
        PageResp<KnowledgeArticleRespDTO> result;
        if (sort == null || "newest".equalsIgnoreCase(sort)) {
            result = knowledgeArticleService.getPublicArticles(page, size, keyword, effectiveCategory);
        } else {
            result = knowledgeArticleService.getPublicArticles(page, size, keyword, effectiveCategory, sort);
        }
        return ApiResp.ok("Lấy danh sách bài viết kiến thức công khai thành công", result);
    }

    /**
     * UC-02: Lấy thông tin chi tiết một bài viết kiến thức công khai theo ID.
     */
    @Operation(summary = "UC-02: Lấy chi tiết bài viết kiến thức theo ID",
            description = "Xem chi tiết bài viết đã xuất bản (published) theo ID. Tự động tăng số lượt xem (viewCount).")
    @GetMapping("/{id}")
    public ApiResp<KnowledgeArticleRespDTO> getPublicArticleById(@PathVariable Integer id) {
        KnowledgeArticleRespDTO article = knowledgeArticleService.getPublicArticleById(id);
        return ApiResp.ok("Lấy chi tiết bài viết kiến thức công khai thành công", article);
    }

    /**
     * UC-02.1: Lấy thông tin chi tiết một bài viết kiến thức công khai theo Slug (chuẩn SEO).
     */
    @Operation(summary = "UC-02: Lấy chi tiết bài viết kiến thức theo Slug (SEO)",
            description = "Xem chi tiết bài viết đã xuất bản (published) theo đường dẫn slug thân thiện. Tự động tăng số lượt xem (viewCount).")
    @GetMapping("/slug/{slug}")
    public ApiResp<KnowledgeArticleRespDTO> getPublicArticleBySlug(@PathVariable String slug) {
        KnowledgeArticleRespDTO article = knowledgeArticleService.getPublicArticleBySlug(slug);
        return ApiResp.ok("Lấy chi tiết bài viết kiến thức theo đường dẫn slug thành công", article);
    }
}
