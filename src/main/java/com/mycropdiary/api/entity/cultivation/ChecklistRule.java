package com.mycropdiary.api.entity.cultivation;

import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.User;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entity biểu diễn bảng [ChecklistRule] (Quy tắc kiểm tra danh mục bằng chứng VietGAP).
 */
@Entity
@Table(name = "ChecklistRule")
public class ChecklistRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "checklist_rule_id", updatable = false, nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id")
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "rule_code", length = 30)
    private String ruleCode;

    @Column(name = "rule_name", nullable = false, length = 160)
    private String ruleName;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "category", length = 80)
    private String category;

    @Column(name = "rule_level", length = 20)
    private String ruleLevel = "MUST";

    @Column(name = "target_crop_type", length = 50)
    private String targetCropType = "ALL";

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.ruleLevel == null || this.ruleLevel.isBlank()) {
            this.ruleLevel = "MUST";
        }
        if (this.targetCropType == null || this.targetCropType.isBlank()) {
            this.targetCropType = "ALL";
        }
    }

    public ChecklistRule() {
    }

    public ChecklistRule(Farm farm, User createdBy, String ruleCode, String ruleName, String description,
                         String category, String ruleLevel, String targetCropType, Boolean isActive) {
        this.farm = farm;
        this.createdBy = createdBy;
        this.ruleCode = ruleCode;
        this.ruleName = ruleName;
        this.description = description;
        this.category = category;
        this.ruleLevel = ruleLevel != null ? ruleLevel : "MUST";
        this.targetCropType = targetCropType != null ? targetCropType : "ALL";
        this.isActive = isActive != null ? isActive : true;
        this.createdAt = Instant.now();
    }

    public ChecklistRule(Farm farm, User createdBy, String ruleName, String description, String category, Boolean isActive) {
        this(farm, createdBy, null, ruleName, description, category, "MUST", "ALL", isActive);
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

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getRuleLevel() {
        return ruleLevel;
    }

    public void setRuleLevel(String ruleLevel) {
        this.ruleLevel = ruleLevel;
    }

    public String getTargetCropType() {
        return targetCropType;
    }

    public void setTargetCropType(String targetCropType) {
        this.targetCropType = targetCropType;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

