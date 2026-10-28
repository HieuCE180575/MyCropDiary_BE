package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.knowledge.KnowledgeArticleRespDTO;

/**
 * Interface định nghĩa các dịch vụ nghiệp vụ xử lý Bài viết Kiến thức Nông nghiệp (Public).
 */
public interface KnowledgeArticleService {

    /**
     * Lấy danh sách các bài viết công khai có hỗ trợ phân trang, tìm kiếm và lọc.
     *
     * @param page vị trí trang (0-indexed)
     * @param size số lượng bản ghi mỗi trang
     * @param keyword từ khóa tìm kiếm theo tiêu đề, tóm tắt hoặc thẻ
     * @param categoryId mã/tên danh mục lọc
     * @return danh sách kết quả đã phân trang dạng DTO
     */
    PageResp<KnowledgeArticleRespDTO> getPublicArticles(int page, int size, String keyword, String categoryId);

    /**
     * Lấy danh sách các bài viết công khai hỗ trợ thêm tùy chọn sắp xếp (newest / popular).
     *
     * @param page vị trí trang (0-indexed)
     * @param size số lượng bản ghi mỗi trang
     * @param keyword từ khóa tìm kiếm
     * @param categoryId mã danh mục
     * @param sort tiêu chí sắp xếp ("newest" hoặc "popular")
     * @return danh sách kết quả đã phân trang dạng DTO
     */
    PageResp<KnowledgeArticleRespDTO> getPublicArticles(int page, int size, String keyword, String categoryId, String sort);

    /**
     * Lấy thông tin chi tiết một bài viết công khai theo ID (tự động tăng viewCount).
     *
     * @param id ID của bài viết
     * @return DTO bài viết tìm thấy
     */
    KnowledgeArticleRespDTO getPublicArticleById(Integer id);

    /**
     * Lấy thông tin chi tiết một bài viết công khai theo Slug (tự động tăng viewCount).
     *
     * @param slug đường dẫn định danh bài viết
     * @return DTO bài viết tìm thấy
     */
    KnowledgeArticleRespDTO getPublicArticleBySlug(String slug);
}
