package com.mycropdiary.api.repository.knowledge;

import com.mycropdiary.api.entity.knowledge.KnowledgeArticle;
import com.mycropdiary.api.entity.knowledge.KnowledgeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository thao tác dữ liệu cơ sở dữ liệu cho bảng KnowledgeArticle.
 */
@Repository
public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Integer>, JpaSpecificationExecutor<KnowledgeArticle> {

    @Query("SELECT k FROM KnowledgeArticle k WHERE LOWER(k.articleStatus) = LOWER(:status)")
    List<KnowledgeArticle> findByStatusString(@Param("status") String status);

    default List<KnowledgeArticle> findByStatus(KnowledgeStatus status) {
        return findByStatusString(status.name());
    }

    Optional<KnowledgeArticle> findBySlug(String slug);

    Optional<KnowledgeArticle> findBySlugAndArticleStatusIgnoreCase(String slug, String status);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Integer id);
}
