package com.mycropdiary.api.entity.cultivation;

import com.mycropdiary.api.entity.FarmMember;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entity biểu diễn bảng [ChecklistResult] (Kết quả và bằng chứng kiểm tra của Checklist).
 */
@Entity
@Table(name = "ChecklistResult")
public class ChecklistResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "checklist_result_id", updatable = false, nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checklist_run_id", nullable = false)
    private ChecklistRun checklistRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checklist_rule_id")
    private ChecklistRule checklistRule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_by")
    private FarmMember checkedBy;

    @Column(name = "rule_code", length = 30)
    private String ruleCode;

    @Column(name = "rule_name", length = 200)
    private String ruleName;

    @Column(name = "category", length = 80)
    private String category;

    @Column(name = "rule_level", length = 20)
    private String ruleLevel;

    @Column(name = "result_status", nullable = false, length = 20)
    private String resultStatus = "pending"; // 'pass', 'fail', 'not_applicable', 'pending'

    @Column(name = "notes", columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    @Column(name = "evidence_image_url", columnDefinition = "NVARCHAR(MAX)")
    private String evidenceImageUrl;

    @Column(name = "evidence_reference_type", length = 50)
    private String evidenceReferenceType;

    @Column(name = "evidence_reference_id")
    private Integer evidenceReferenceId;

    @Column(name = "is_auto_evaluated", nullable = false)
    private Boolean isAutoEvaluated = false;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;

    @PrePersist
    void onCreate() {
        if (this.checkedAt == null) {
            this.checkedAt = Instant.now();
        }
        if (this.resultStatus == null || this.resultStatus.isBlank()) {
            this.resultStatus = "pending";
        }
        if (this.isAutoEvaluated == null) {
            this.isAutoEvaluated = false;
        }
    }

    public ChecklistResult() {
    }

    public ChecklistResult(ChecklistRun checklistRun, FarmMember checkedBy, String resultStatus,
                           String notes, String evidenceImageUrl, Instant checkedAt) {
        this.checklistRun = checklistRun;
        this.checkedBy = checkedBy;
        this.resultStatus = resultStatus;
        this.notes = notes;
        this.evidenceImageUrl = evidenceImageUrl;
        this.checkedAt = checkedAt != null ? checkedAt : Instant.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public ChecklistRun getChecklistRun() {
        return checklistRun;
    }

    public void setChecklistRun(ChecklistRun checklistRun) {
        this.checklistRun = checklistRun;
    }

    public FarmMember getCheckedBy() {
        return checkedBy;
    }

    public void setCheckedBy(FarmMember checkedBy) {
        this.checkedBy = checkedBy;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getEvidenceImageUrl() {
        return evidenceImageUrl;
    }

    public void setEvidenceImageUrl(String evidenceImageUrl) {
        this.evidenceImageUrl = evidenceImageUrl;
    }

    public ChecklistRule getChecklistRule() {
        return checklistRule;
    }

    public void setChecklistRule(ChecklistRule checklistRule) {
        this.checklistRule = checklistRule;
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

    public String getEvidenceReferenceType() {
        return evidenceReferenceType;
    }

    public void setEvidenceReferenceType(String evidenceReferenceType) {
        this.evidenceReferenceType = evidenceReferenceType;
    }

    public Integer getEvidenceReferenceId() {
        return evidenceReferenceId;
    }

    public void setEvidenceReferenceId(Integer evidenceReferenceId) {
        this.evidenceReferenceId = evidenceReferenceId;
    }

    public Boolean getIsAutoEvaluated() {
        return isAutoEvaluated;
    }

    public void setIsAutoEvaluated(Boolean autoEvaluated) {
        isAutoEvaluated = autoEvaluated;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Instant checkedAt) {
        this.checkedAt = checkedAt;
    }
}

