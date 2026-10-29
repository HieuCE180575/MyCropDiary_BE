package com.mycropdiary.api.entity.cultivation;

import com.mycropdiary.api.entity.User;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entity biểu diễn bảng [TaskAssignment] (Phân công công việc cho công nhân).
 */
@Entity
@Table(name = "TaskAssignment")
public class TaskAssignment {

    @EmbeddedId
    private TaskAssignmentId id = new TaskAssignmentId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("farmTaskId")
    @JoinColumn(name = "farm_task_id", nullable = false)
    private FarmTask farmTask;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("farmWorkerId")
    @JoinColumn(name = "farm_worker_id", nullable = false)
    private FarmWorker farmWorker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "assignment_status", nullable = false, length = 20)
    private String assignmentStatus = "assigned"; // assigned, accepted, completed, cancelled

    @PrePersist
    void onCreate() {
        if (assignedAt == null) {
            assignedAt = Instant.now();
        }
    }

    public TaskAssignment() {}

    public TaskAssignment(FarmTask farmTask, FarmWorker farmWorker, User assignedBy, String assignmentStatus) {
        this.farmTask = farmTask;
        this.farmWorker = farmWorker;
        this.assignedBy = assignedBy;
        if (assignmentStatus != null) {
            this.assignmentStatus = assignmentStatus.toLowerCase();
        }
        if (farmTask != null && farmWorker != null) {
            this.id = new TaskAssignmentId(farmTask.getId(), farmWorker.getId());
        }
    }

    public TaskAssignmentId getId() { return id; }
    public void setId(TaskAssignmentId id) { this.id = id; }

    public FarmTask getFarmTask() { return farmTask; }
    public void setFarmTask(FarmTask farmTask) {
        this.farmTask = farmTask;
        if (farmTask != null) {
            this.id.setFarmTaskId(farmTask.getId());
        }
    }

    public FarmWorker getFarmWorker() { return farmWorker; }
    public void setFarmWorker(FarmWorker farmWorker) {
        this.farmWorker = farmWorker;
        if (farmWorker != null) {
            this.id.setFarmWorkerId(farmWorker.getId());
        }
    }

    public User getAssignedBy() { return assignedBy; }
    public void setAssignedBy(User assignedBy) { this.assignedBy = assignedBy; }

    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }

    public String getAssignmentStatus() { return assignmentStatus; }
    public void setAssignmentStatus(String assignmentStatus) {
        this.assignmentStatus = assignmentStatus != null ? assignmentStatus.toLowerCase() : "assigned";
    }
}
