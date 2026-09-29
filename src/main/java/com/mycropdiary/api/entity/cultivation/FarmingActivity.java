package com.mycropdiary.api.entity.cultivation;

import com.mycropdiary.api.entity.FarmMember;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity biểu diễn bảng dbo.FarmingActivity (Hoạt động canh tác / Nhật ký canh tác).
 */
@Entity
@Table(name = "FarmingActivity", schema = "dbo")
public class FarmingActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FarmingActivityID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CropSeasonID", nullable = false)
    private CropSeason cropSeason;

    @Column(name = "FarmTaskID")
    private Long farmTaskId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SupervisedByMemberID", nullable = false)
    private FarmMember supervisedByMember;

    @Column(name = "ActivityType", nullable = false, length = 40)
    private String activityType;

    @Column(name = "ActivityName", nullable = false, length = 200)
    private String activityName;

    @Column(name = "StartedAt", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "EndedAt")
    private LocalDateTime endedAt;

    @Column(name = "Description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "ResultNotes", columnDefinition = "NVARCHAR(MAX)")
    private String resultNotes;

    @Column(name = "WeatherNotes", length = 1000)
    private String weatherNotes;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    public FarmingActivity() { }

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CropSeason getCropSeason() { return cropSeason; }
    public void setCropSeason(CropSeason cropSeason) { this.cropSeason = cropSeason; }

    public Long getFarmTaskId() { return farmTaskId; }
    public void setFarmTaskId(Long farmTaskId) { this.farmTaskId = farmTaskId; }

    public FarmMember getSupervisedByMember() { return supervisedByMember; }
    public void setSupervisedByMember(FarmMember supervisedByMember) { this.supervisedByMember = supervisedByMember; }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getResultNotes() { return resultNotes; }
    public void setResultNotes(String resultNotes) { this.resultNotes = resultNotes; }

    public String getWeatherNotes() { return weatherNotes; }
    public void setWeatherNotes(String weatherNotes) { this.weatherNotes = weatherNotes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
