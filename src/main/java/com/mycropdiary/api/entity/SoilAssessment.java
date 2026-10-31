package com.mycropdiary.api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity biểu diễn bảng [SoilAssessment] (Đánh giá chỉ tiêu thổ nhưỡng, độ ẩm, dinh dưỡng theo Block).
 * Khớp 100% lược đồ database [SoilAssessment] trong MyCropDiary_SQLServer.sql.
 */
@Entity
@Table(name = "SoilAssessment")
public class SoilAssessment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "soil_assessment_id", updatable = false, nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "block_id", nullable = false)
    private Block block;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conducted_by")
    private FarmMember conductedBy;

    @Column(name = "soil_ph", precision = 5, scale = 2)
    private BigDecimal soilPh;

    @Column(name = "moisture_percent", precision = 5, scale = 2)
    private BigDecimal moisturePercent;

    @Column(name = "nutrient_data", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String nutrientData = "{}";

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
        if (nutrientData == null || nutrientData.isBlank()) {
            nutrientData = "{}";
        }
    }

    public SoilAssessment() { }

    public SoilAssessment(Block block, FarmMember conductedBy, BigDecimal soilPh, BigDecimal moisturePercent,
                          String nutrientData, String notes, String imageUrl, Instant assessedAt) {
        this.block = block;
        this.conductedBy = conductedBy;
        this.soilPh = soilPh;
        this.moisturePercent = moisturePercent;
        this.nutrientData = nutrientData != null && !nutrientData.isBlank() ? nutrientData : "{}";
        this.notes = notes;
        this.imageUrl = imageUrl;
        this.assessedAt = assessedAt != null ? assessedAt : Instant.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Block getBlock() { return block; }
    public void setBlock(Block block) { this.block = block; }

    public FarmMember getConductedBy() { return conductedBy; }
    public void setConductedBy(FarmMember conductedBy) { this.conductedBy = conductedBy; }

    public BigDecimal getSoilPh() { return soilPh; }
    public void setSoilPh(BigDecimal soilPh) { this.soilPh = soilPh; }

    public BigDecimal getMoisturePercent() { return moisturePercent; }
    public void setMoisturePercent(BigDecimal moisturePercent) { this.moisturePercent = moisturePercent; }

    public String getNutrientData() { return nutrientData; }
    public void setNutrientData(String nutrientData) { this.nutrientData = nutrientData; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Instant getAssessedAt() { return assessedAt; }
    public void setAssessedAt(Instant assessedAt) { this.assessedAt = assessedAt; }
}
