package com.mycropdiary.api.service.admin;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.knowledge.CreateKnowledgeArticleReq;
import com.mycropdiary.api.dto.knowledge.KnowledgeArticleRespDTO;
import com.mycropdiary.api.dto.knowledge.UpdateKnowledgeArticleReq;
import com.mycropdiary.api.dto.knowledge.UpdateKnowledgeStatusReq;
import org.springframework.data.domain.Pageable;

/**
 * Interface định nghĩa các dịch vụ nghiệp vụ Quản trị Bài viết Kiến thức Nông nghiệp (Admin CMS).
 */
public interface AdminKnowledgeArticleService {

    /**
     * Lấy danh sách tất cả bài viết dành cho Admin (bao gồm draft, published, archived).
     */
    PageResp<KnowledgeArticleRespDTO> getAdminArticles(String status, String category, String search, Pageable pageable);

    /**
     * Xem chi tiết bài viết bất kỳ theo ID.
     */
    KnowledgeArticleRespDTO getAdminArticleById(Integer id);

    /**
     * Tạo bài viết kiến thức mới.
     */
    KnowledgeArticleRespDTO createArticle(Integer adminUserId, CreateKnowledgeArticleReq request);

    /**
     * Cập nhật thông tin bài viết kiến thức.
     */
    KnowledgeArticleRespDTO updateArticle(Integer adminUserId, Integer id, UpdateKnowledgeArticleReq request);

    /**
     * Đổi trạng thái bài viết (draft, published, archived).
     */
    KnowledgeArticleRespDTO updateArticleStatus(Integer adminUserId, Integer id, UpdateKnowledgeStatusReq request);

    /**
     * Xóa bài viết kiến thức.
     */
    void deleteArticle(Integer adminUserId, Integer id);
}

