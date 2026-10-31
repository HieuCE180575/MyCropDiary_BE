package com.mycropdiary.api.entity.cultivation;

import com.mycropdiary.api.entity.Farm;
import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Entity biễu diễn bảng [AssessmentCriterion] (Tiêu chuẩn đánh giá nội bộ VietGAP theo TCVN 11892-1:2017).
 */
@Entity
@Table(name = "AssessmentCriterion", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"farm_id", "criterion_name"})
})
public class AssessmentCriterion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assessment_criterion_id", updatable = false, nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Column(name = "criterion_name", nullable = false, length = 160)
    private String criterionName;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "max_score", nullable = false, precision = 8, scale = 2)
    private BigDecimal maxScore = new BigDecimal("100.00");

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public AssessmentCriterion() {
    }

    public AssessmentCriterion(Farm farm, String criterionName, String description, BigDecimal maxScore, Boolean isActive) {
        this.farm = farm;
        this.criterionName = criterionName;
        this.description = description;
        this.maxScore = maxScore != null ? maxScore : new BigDecimal("100.00");
        this.isActive = isActive != null ? isActive : true;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Farm getFarm() {
        return farm;
    }

    public void setFarm(Farm farm) {
        this.farm = farm;
    }

    public String getCriterionName() {
        return criterionName;
    }

    public void setCriterionName(String criterionName) {
        this.criterionName = criterionName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}
