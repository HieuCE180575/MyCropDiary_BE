package com.mycropdiary.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entity biểu diễn bảng [WaterSource] (Nguồn nước phục vụ tưới tiêu trang trại).
 */
@Entity
@Table(name = "WaterSource")
public class WaterSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "water_source_id", updatable = false, nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Column(name = "source_name", length = 120, nullable = false)
    private String sourceName;

    @Column(name = "source_type", length = 60)
    private String sourceType;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "image_url", columnDefinition = "NVARCHAR(MAX)")
    private String imageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public WaterSource() {}

    public WaterSource(Farm farm, String sourceName, String sourceType, String description, String imageUrl) {
        this.farm = farm;
        this.sourceName = sourceName;
        this.sourceType = sourceType;
        this.description = description;
        this.imageUrl = imageUrl;
        this.createdAt = Instant.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

