package com.mycropdiary.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entity biểu diễn bảng [ZoneRiskAssessment] (Đánh giá rủi ro môi trường và sản xuất theo Zone).
 * Khớp 100% lược đồ database [ZoneRiskAssessment] trong MyCropDiary_SQLServer.sql.
 */
@Entity
@Table(name = "ZoneRiskAssessment")
public class ZoneRiskAssessment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "zone_risk_assessment_id", updatable = false, nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessed_by")
    private FarmMember assessedBy;

    @Column(name = "risk_type", nullable = false, length = 80)
    private String riskType;

    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel; // 'low', 'medium', 'high', 'critical'

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "mitigation_plan", columnDefinition = "NVARCHAR(MAX)")
    private String mitigationPlan; // Biện pháp kiểm soát rủi ro (Risk Control Measures)

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

    public ZoneRiskAssessment() { }

    public ZoneRiskAssessment(Zone zone, FarmMember assessedBy, String riskType, String riskLevel,
                              String description, String mitigationPlan, String imageUrl, Instant assessedAt) {
        this.zone = zone;
        this.assessedBy = assessedBy;
        this.riskType = riskType;
        this.riskLevel = riskLevel != null ? riskLevel.toLowerCase().trim() : "low";
        this.description = description;
        this.mitigationPlan = mitigationPlan;
        this.imageUrl = imageUrl;
        this.assessedAt = assessedAt != null ? assessedAt : Instant.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Zone getZone() { return zone; }
    public void setZone(Zone zone) { this.zone = zone; }

    public FarmMember getAssessedBy() { return assessedBy; }
    public void setAssessedBy(FarmMember assessedBy) { this.assessedBy = assessedBy; }

    public String getRiskType() { return riskType; }
    public void setRiskType(String riskType) { this.riskType = riskType; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel != null ? riskLevel.toLowerCase().trim() : "low"; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMitigationPlan() { return mitigationPlan; }
    public void setMitigationPlan(String mitigationPlan) { this.mitigationPlan = mitigationPlan; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Instant getAssessedAt() { return assessedAt; }
    public void setAssessedAt(Instant assessedAt) { this.assessedAt = assessedAt; }
}
