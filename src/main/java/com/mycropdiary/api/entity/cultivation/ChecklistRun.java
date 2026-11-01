package com.mycropdiary.api.entity.cultivation;

import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.FarmMember;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity biểu diễn bảng [ChecklistRun] (Lượt thực thi danh mục kiểm tra VietGAP).
 */
@Entity
@Table(name = "ChecklistRun")
public class ChecklistRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "checklist_run_id", updatable = false, nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checklist_rule_id", nullable = false)
    private ChecklistRule checklistRule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_season_id")
    private CropSeason cropSeason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id")
    private Block block;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private FarmMember assignedTo;

    @Column(name = "run_status", nullable = false, length = 20)
    private String runStatus = "open"; // 'open','in_progress','completed','cancelled'

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "checklistRun", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChecklistResult> results = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.runStatus == null || this.runStatus.isBlank()) {
            this.runStatus = "open";
        }
    }

    public ChecklistRun() {
    }

    public ChecklistRun(ChecklistRule checklistRule, CropSeason cropSeason, Block block, FarmMember assignedTo,
                        String runStatus, Instant scheduledAt, Instant completedAt) {
        this.checklistRule = checklistRule;
        this.cropSeason = cropSeason;
        this.block = block;
        this.assignedTo = assignedTo;
        this.runStatus = runStatus != null ? runStatus : "open";
        this.scheduledAt = scheduledAt;
        this.completedAt = completedAt;
        this.createdAt = Instant.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public ChecklistRule getChecklistRule() {
        return checklistRule;
    }

    public void setChecklistRule(ChecklistRule checklistRule) {
        this.checklistRule = checklistRule;
    }

    public CropSeason getCropSeason() {
        return cropSeason;
    }

    public void setCropSeason(CropSeason cropSeason) {
        this.cropSeason = cropSeason;
    }

    public Block getBlock() {
        return block;
    }

    public void setBlock(Block block) {
        this.block = block;
    }

    public FarmMember getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(FarmMember assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getRunStatus() {
        return runStatus;
    }

    public void setRunStatus(String runStatus) {
        this.runStatus = runStatus;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<ChecklistResult> getResults() {
        return results;
    }

    public void setResults(List<ChecklistResult> results) {
        this.results = results;
    }

    public void addResult(ChecklistResult result) {
        this.results.add(result);
        result.setChecklistRun(this);
    }
}

