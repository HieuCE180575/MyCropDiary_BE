package com.mycropdiary.api.repository.knowledge;

import com.mycropdiary.api.entity.knowledge.KnowledgeArticle;
import com.mycropdiary.api.entity.knowledge.KnowledgeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Long> {

    List<KnowledgeArticle> findByStatus(KnowledgeStatus status);

    List<KnowledgeArticle> findByCategory(String category);

    List<KnowledgeArticle> findByStatusAndCategory(KnowledgeStatus status, String category);

    Optional<KnowledgeArticle> findBySlug(String slug);
}
