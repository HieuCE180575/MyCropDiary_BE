package com.mycropdiary.api.entity.cultivation;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Entity biểu diễn bảng dbo.ActivityWorker (Phân công công nhân trực tiếp thực hiện nhật ký canh tác).
 */
@Entity
@Table(name = "ActivityWorker", schema = "dbo")
@IdClass(ActivityWorkerId.class)
public class ActivityWorker {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FarmingActivityID", nullable = false)
    private FarmingActivity farmingActivity;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FarmWorkerID", nullable = false)
    private FarmWorker farmWorker;

    @Column(name = "WorkHours", precision = 8, scale = 2)
    private BigDecimal workHours;

    @Column(name = "Notes", length = 500)
    private String notes;

    public ActivityWorker() { }

    public ActivityWorker(FarmingActivity farmingActivity, FarmWorker farmWorker, BigDecimal workHours, String notes) {
        this.farmingActivity = farmingActivity;
        this.farmWorker = farmWorker;
        this.workHours = workHours;
        this.notes = notes;
    }

    public FarmingActivity getFarmingActivity() { return farmingActivity; }
    public void setFarmingActivity(FarmingActivity farmingActivity) { this.farmingActivity = farmingActivity; }

    public FarmWorker getFarmWorker() { return farmWorker; }
    public void setFarmWorker(FarmWorker farmWorker) { this.farmWorker = farmWorker; }

    public BigDecimal getWorkHours() { return workHours; }
    public void setWorkHours(BigDecimal workHours) { this.workHours = workHours; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
