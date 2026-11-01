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
    @JoinColumn(name = "checked_by")
    private FarmMember checkedBy;

    @Column(name = "result_status", nullable = false, length = 20)
    private String resultStatus; // 'pass', 'fail', 'not_applicable'

    @Column(name = "notes", columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    @Column(name = "evidence_image_url", columnDefinition = "NVARCHAR(MAX)")
    private String evidenceImageUrl;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;

    @PrePersist
    void onCreate() {
        if (this.checkedAt == null) {
            this.checkedAt = Instant.now();
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

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Instant checkedAt) {
        this.checkedAt = checkedAt;
    }
}

