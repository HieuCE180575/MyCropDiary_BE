package com.mycropdiary.api.entity.knowledge;

import com.mycropdiary.api.entity.BaseEntity;
import com.mycropdiary.api.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "KnowledgeArticle")
public class KnowledgeArticle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "knowledge_article_id", updatable = false, nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "title", nullable = false, length = 220)
    private String title;

    @Column(name = "slug", nullable = false, unique = true, length = 240)
    private String slug;

    @Column(name = "summary", columnDefinition = "NVARCHAR(MAX)")
    private String summary;

    @Column(name = "body", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String body;

    @Column(name = "cover_image_url", columnDefinition = "NVARCHAR(MAX)")
    private String coverImageUrl;

    @Column(name = "article_status", nullable = false, length = 20)
    private String articleStatus = "draft"; // draft, published, archived

    @Column(name = "published_at")
    private Instant publishedAt;

    public KnowledgeArticle() { }

    public KnowledgeArticle(User createdBy, String title, String slug, String body) {
        this.createdBy = createdBy;
        this.title = title;
        this.slug = slug;
        this.body = body;
        this.articleStatus = "draft";
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

    public String getArticleStatus() { return articleStatus; }
    public void setArticleStatus(String articleStatus) {
        this.articleStatus = articleStatus != null ? articleStatus.toLowerCase() : "draft";
    }

    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }

    // --- Backward compatibility helpers ---
    public User getCreatedByUser() { return createdBy; }
    public void setCreatedByUser(User user) { this.createdBy = user; }

    public String getContent() { return body; }
    public void setContent(String content) { this.body = content; }

    public KnowledgeStatus getStatus() {
        if ("published".equalsIgnoreCase(articleStatus)) return KnowledgeStatus.PUBLISHED;
        if ("archived".equalsIgnoreCase(articleStatus)) return KnowledgeStatus.ARCHIVED;
        return KnowledgeStatus.DRAFT;
    }
    public void setStatus(KnowledgeStatus status) {
        this.articleStatus = status != null ? status.name().toLowerCase() : "draft";
    }

    public String getCategory() { return null; }
    public void setCategory(String category) { }

    public String getSourceUrl() { return null; }
    public void setSourceUrl(String sourceUrl) { }

    public Boolean getIsPublic() { return "published".equalsIgnoreCase(articleStatus); }
    public void setIsPublic(Boolean isPublic) { }

    public LocalDateTime getPublishedAtLocalDateTime() {
        return publishedAt != null ? LocalDateTime.ofInstant(publishedAt, ZoneId.systemDefault()) : null;
    }
    public void setPublishedAtLocalDateTime(LocalDateTime ldt) {
        this.publishedAt = ldt != null ? ldt.atZone(ZoneId.systemDefault()).toInstant() : null;
    }
}
