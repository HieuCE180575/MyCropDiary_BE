package com.mycropdiary.api.repository.knowledge;

import com.mycropdiary.api.entity.knowledge.KnowledgeArticle;
import com.mycropdiary.api.entity.knowledge.KnowledgeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository thao tác dữ liệu cơ sở dữ liệu cho bảng KnowledgeArticle.
 * Hỗ trợ các phương thức CRUD chuẩn của JPA và truy vấn động Specification.
 */
@Repository
public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Long>, JpaSpecificationExecutor<KnowledgeArticle> {

    /**
     * Tìm danh sách bài viết theo trạng thái.
     */
    List<KnowledgeArticle> findByStatus(KnowledgeStatus status);

    /**
     * Tìm danh sách bài viết theo danh mục.
     */
    List<KnowledgeArticle> findByCategory(String category);

    /**
     * Tìm danh sách bài viết theo trạng thái và danh mục.
     */
    List<KnowledgeArticle> findByStatusAndCategory(KnowledgeStatus status, String category);

    /**
     * Tìm bài viết theo đường dẫn duy nhất (Slug).
     */
    Optional<KnowledgeArticle> findBySlug(String slug);
}
