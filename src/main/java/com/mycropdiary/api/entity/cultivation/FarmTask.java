package com.mycropdiary.api.entity.cultivation;

import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.Zone;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity biểu diễn bảng [FarmTask] (Công việc và nhiệm vụ sản xuất nông trại).
 */
@Entity
@Table(name = "FarmTask")
public class FarmTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "farm_task_id", updatable = false, nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_season_id")
    private CropSeason cropSeason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id")
    private Block block;

    @Column(name = "task_name", nullable = false, length = 160)
    private String taskName;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "task_type", length = 60)
    private String taskType;

    @Column(name = "priority", nullable = false, length = 20)
    private String priority = "normal"; // low, normal, high, urgent

    @Column(name = "status", nullable = false, length = 20)
    private String status = "planned"; // planned, in_progress, completed, cancelled

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "farmTask", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskAssignment> assignments = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (priority == null) {
            priority = "normal";
        } else {
            priority = priority.toLowerCase();
        }
        if (status == null) {
            status = "planned";
        } else {
            status = status.toLowerCase();
        }
    }

    public FarmTask() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }

    public CropSeason getCropSeason() { return cropSeason; }
    public void setCropSeason(CropSeason cropSeason) { this.cropSeason = cropSeason; }

    public Zone getZone() { return zone; }
    public void setZone(Zone zone) { this.zone = zone; }

    public Block getBlock() { return block; }
    public void setBlock(Block block) { this.block = block; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) {
        this.priority = priority != null ? priority.toLowerCase() : "normal";
    }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status != null ? status.toLowerCase() : "planned";
    }

    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }

    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public List<TaskAssignment> getAssignments() { return assignments; }
    public void setAssignments(List<TaskAssignment> assignments) { this.assignments = assignments; }

    public void addAssignment(TaskAssignment assignment) {
        assignments.add(assignment);
        assignment.setFarmTask(this);
    }

    public void removeAssignment(TaskAssignment assignment) {
        assignments.remove(assignment);
        assignment.setFarmTask(null);
    }
}

