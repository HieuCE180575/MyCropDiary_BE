package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.knowledge.KnowledgeArticleResponseDTO;

/**
 * Interface định nghĩa các dịch vụ nghiệp vụ xử lý Bài viết Kiến thức Nông nghiệp.
 */
public interface KnowledgeArticleService {

    /**
     * Lấy danh sách các bài viết công khai có hỗ trợ phân trang, tìm kiếm và lọc.
     *
     * @param page vị trí trang (0-indexed)
     * @param size số lượng bản ghi mỗi trang
     * @param keyword từ khóa tìm kiếm theo tiêu đề hoặc tóm tắt
     * @param categoryId mã/tên danh mục lọc
     * @return danh sách kết quả đã phân trang dạng DTO
     */
    PageResponse<KnowledgeArticleResponseDTO> getPublicArticles(int page, int size, String keyword, String categoryId);

    /**
     * Lấy thông tin chi tiết một bài viết công khai theo ID.
     *
     * @param id ID của bài viết
     * @return DTO bài viết tìm thấy
     */
    KnowledgeArticleResponseDTO getPublicArticleById(Long id);
}
