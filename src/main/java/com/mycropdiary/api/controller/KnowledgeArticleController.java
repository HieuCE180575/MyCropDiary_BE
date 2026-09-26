package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResponse;
import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.knowledge.KnowledgeArticleResponseDTO;
import com.mycropdiary.api.service.KnowledgeArticleService;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý các API công khai liên quan đến Bài viết Kiến thức Nông nghiệp (Knowledge Articles).
 * Cho phép khách và người dùng xem danh sách, tìm kiếm, lọc và xem chi tiết bài viết.
 */
@RestController
@RequestMapping("/api/v1/knowledge/articles")
public class KnowledgeArticleController {

    private final KnowledgeArticleService knowledgeArticleService;

    public KnowledgeArticleController(KnowledgeArticleService knowledgeArticleService) {
        this.knowledgeArticleService = knowledgeArticleService;
    }

    /**
     * UC-01: Lấy danh sách các bài viết kiến thức công khai (Có phân trang, tìm kiếm và lọc theo danh mục).
     *
     * @param page số trang muốn lấy (mặc định: 0)
     * @param size số lượng bài viết mỗi trang (mặc định: 10)
     * @param keyword từ khóa tìm kiếm theo tiêu đề hoặc tóm tắt (không bắt buộc)
     * @param categoryId ID hoặc mã danh mục lọc bài viết (không bắt buộc)
     * @param category tên danh mục lọc bài viết (dùng thay thế cho categoryId)
     * @return ApiResponse chứa PageResponse các KnowledgeArticleResponseDTO
     */
    @GetMapping
    public ApiResponse<PageResponse<KnowledgeArticleResponseDTO>> getPublicArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String category
    ) {
        // Ưu tiên dùng categoryId nếu có, nếu không thì lấy tham số category
        String effectiveCategory = categoryId != null ? categoryId : category;
        PageResponse<KnowledgeArticleResponseDTO> result = knowledgeArticleService.getPublicArticles(page, size, keyword, effectiveCategory);
        return ApiResponse.ok("Lấy danh sách bài viết kiến thức công khai thành công", result);
    }

    /**
     * UC-02: Lấy thông tin chi tiết một bài viết kiến thức công khai theo ID.
     *
     * @param id ID của bài viết cần xem
     * @return ApiResponse chứa chi tiết bài viết KnowledgeArticleResponseDTO
     */
    @GetMapping("/{id}")
    public ApiResponse<KnowledgeArticleResponseDTO> getPublicArticleById(@PathVariable Long id) {
        KnowledgeArticleResponseDTO article = knowledgeArticleService.getPublicArticleById(id);
        return ApiResponse.ok("Lấy chi tiết bài viết kiến thức công khai thành công", article);
    }
}
