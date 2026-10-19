package com.mycropdiary.api.entity.cultivation;

import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.Zone;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Entity biểu diễn bảng [FarmingActivity] (Hoạt động canh tác / Nhật ký canh tác).
 * Khớp 100% với ERD mới.
 */
@Entity
@Table(name = "FarmingActivity")
public class FarmingActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "farming_activity_id", updatable = false, nullable = false)
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

    @Column(name = "farm_task_id")
    private Integer farmTaskId;

    @Column(name = "activity_name", nullable = false, length = 160)
    private String activityName;

    @Column(name = "activity_type", length = 60)
    private String activityType;

    @Column(name = "activity_notes", columnDefinition = "NVARCHAR(MAX)")
    private String activityNotes;

    @Column(name = "performed_at", nullable = false)
    private Instant performedAt;

    @Column(name = "image_url", columnDefinition = "NVARCHAR(MAX)")
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (this.performedAt == null) {
            this.performedAt = Instant.now();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public FarmingActivity() { }

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

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public String getActivityNotes() { return activityNotes; }
    public void setActivityNotes(String activityNotes) { this.activityNotes = activityNotes; }

    public Instant getPerformedAt() { return performedAt; }
    public void setPerformedAt(Instant performedAt) { this.performedAt = performedAt; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    // --- Backward compatibility helpers ---
    public LocalDateTime getStartedAt() {
        return performedAt != null ? LocalDateTime.ofInstant(performedAt, ZoneId.systemDefault()) : null;
    }
    public void setStartedAt(LocalDateTime startedAt) {
        if (startedAt != null) {
            this.performedAt = startedAt.atZone(ZoneId.systemDefault()).toInstant();
        }
    }

    public LocalDateTime getEndedAt() { return getStartedAt(); }
    public void setEndedAt(LocalDateTime endedAt) { }

    public String getDescription() { return activityNotes; }
    public void setDescription(String description) { this.activityNotes = description; }

    public String getResultNotes() { return activityNotes; }
    public void setResultNotes(String resultNotes) { this.activityNotes = resultNotes; }

    public String getWeatherNotes() { return null; }
    public void setWeatherNotes(String weatherNotes) { }

    public Integer getFarmTaskId() { return farmTaskId; }
    public void setFarmTaskId(Integer farmTaskId) { this.farmTaskId = farmTaskId; }

    public FarmMember getSupervisedByMember() { return null; }
    public void setSupervisedByMember(FarmMember supervisedByMember) { }
}
