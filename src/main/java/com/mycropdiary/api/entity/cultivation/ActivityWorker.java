package com.mycropdiary.api.entity.cultivation;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Entity biểu diễn bảng [ActivityWorker] (Phân công công nhân trực tiếp thực hiện nhật ký canh tác).
 * Khớp 100% với ERD mới.
 */
@Entity
@Table(name = "ActivityWorker")
@IdClass(ActivityWorkerId.class)
public class ActivityWorker {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farming_activity_id", nullable = false)
    private FarmingActivity farmingActivity;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_worker_id", nullable = false)
    private FarmWorker farmWorker;

    @Column(name = "participation_role", length = 60)
    private String participationRole;

    public ActivityWorker() { }

    public ActivityWorker(FarmingActivity farmingActivity, FarmWorker farmWorker, String participationRole) {
        this.farmingActivity = farmingActivity;
        this.farmWorker = farmWorker;
        this.participationRole = participationRole;
    }

    public ActivityWorker(FarmingActivity farmingActivity, FarmWorker farmWorker, BigDecimal workHours, String notes) {
        this.farmingActivity = farmingActivity;
        this.farmWorker = farmWorker;
        this.participationRole = notes;
    }

    public FarmingActivity getFarmingActivity() { return farmingActivity; }
    public void setFarmingActivity(FarmingActivity farmingActivity) { this.farmingActivity = farmingActivity; }

    public FarmWorker getFarmWorker() { return farmWorker; }
    public void setFarmWorker(FarmWorker farmWorker) { this.farmWorker = farmWorker; }

    public String getParticipationRole() { return participationRole; }
    public void setParticipationRole(String participationRole) { this.participationRole = participationRole; }

    // --- Backward compatibility helpers ---
    public BigDecimal getWorkHours() { return null; }
    public void setWorkHours(BigDecimal workHours) { }

    public String getNotes() { return participationRole; }
    public void setNotes(String notes) { this.participationRole = notes; }
}
