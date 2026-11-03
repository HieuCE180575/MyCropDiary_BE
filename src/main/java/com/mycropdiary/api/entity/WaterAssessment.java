package com.mycropdiary.api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity biểu diễn bảng [WaterAssessment] (Kiểm tra chất lượng mẫu nước tưới tiêu theo Block/Farm).
 * Khớp 100% schema [WaterAssessment] trong MyCropDiary_SQLServer.sql.
 */
@Entity
@Table(name = "WaterAssessment")
public class WaterAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "water_assessment_id", updatable = false, nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "water_source_id", nullable = false)
    private WaterSource waterSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id")
    private Block block;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conducted_by")
    private FarmMember conductedBy;

    @Column(name = "water_ph", precision = 5, scale = 2)
    private BigDecimal waterPh;

    @Column(name = "temperature_c", precision = 5, scale = 2)
    private BigDecimal temperatureC;

    @Column(name = "quality_status", length = 30)
    private String qualityStatus;

    @Column(name = "notes", columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    @Column(name = "image_url", columnDefinition = "NVARCHAR(MAX)")
    private String imageUrl;

    @Column(name = "assessed_at", nullable = false)
    private Instant assessedAt;

    @PrePersist
    void onCreate() {
        if (assessedAt == null) {
            assessedAt = Instant.now();
        }
    }

    public WaterAssessment() {}

    public WaterAssessment(WaterSource waterSource, Block block, FarmMember conductedBy,
                           BigDecimal waterPh, BigDecimal temperatureC, String qualityStatus,
                           String notes, String imageUrl, Instant assessedAt) {
        this.waterSource = waterSource;
        this.block = block;
        this.conductedBy = conductedBy;
        this.waterPh = waterPh;
        this.temperatureC = temperatureC;
        this.qualityStatus = qualityStatus;
        this.notes = notes;
        this.imageUrl = imageUrl;
        this.assessedAt = assessedAt != null ? assessedAt : Instant.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public WaterSource getWaterSource() { return waterSource; }
    public void setWaterSource(WaterSource waterSource) { this.waterSource = waterSource; }

    public Block getBlock() { return block; }
    public void setBlock(Block block) { this.block = block; }

    public FarmMember getConductedBy() { return conductedBy; }
    public void setConductedBy(FarmMember conductedBy) { this.conductedBy = conductedBy; }

    public BigDecimal getWaterPh() { return waterPh; }
    public void setWaterPh(BigDecimal waterPh) { this.waterPh = waterPh; }

    public BigDecimal getTemperatureC() { return temperatureC; }
    public void setTemperatureC(BigDecimal temperatureC) { this.temperatureC = temperatureC; }

    public String getQualityStatus() { return qualityStatus; }
    public void setQualityStatus(String qualityStatus) { this.qualityStatus = qualityStatus; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Instant getAssessedAt() { return assessedAt; }
    public void setAssessedAt(Instant assessedAt) { this.assessedAt = assessedAt; }
}

