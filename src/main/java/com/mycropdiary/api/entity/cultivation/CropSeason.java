package com.mycropdiary.api.entity.cultivation;

import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entity biểu diễn bảng [CropSeason] (Mùa vụ canh tác).
 * Khớp 100% với ERD mới.
 */
@Entity
@Table(name = "CropSeason")
public class CropSeason {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "crop_season_id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_category_id", nullable = false)
    private CropCategory cropCategory;

    @Column(name = "season_name", nullable = false, length = 120)
    private String seasonName;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "season_status", nullable = false, length = 20)
    private String seasonStatus = "planned"; // planned, active, completed, cancelled

    @Column(name = "notes", columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public CropSeason() { }

    public CropSeason(Farm farm, CropCategory cropCategory, String seasonName, LocalDate startDate) {
        this.farm = farm;
        this.cropCategory = cropCategory;
        this.seasonName = seasonName;
        this.startDate = startDate;
        this.seasonStatus = "active";
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }

    public CropCategory getCropCategory() { return cropCategory; }
    public void setCropCategory(CropCategory cropCategory) { this.cropCategory = cropCategory; }

    public UUID getCropCategoryId() {
        return cropCategory != null ? cropCategory.getId() : null;
    }

    public String getSeasonName() { return seasonName; }
    public void setSeasonName(String seasonName) { this.seasonName = seasonName; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getSeasonStatus() { return seasonStatus; }
    public void setSeasonStatus(String seasonStatus) {
        this.seasonStatus = seasonStatus != null ? seasonStatus.toLowerCase() : "planned";
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    // --- Backward compatibility helpers ---
    public String getStatus() { return seasonStatus; }
    public void setStatus(String status) { setSeasonStatus(status); }

    public String getSeasonCode() { return seasonName; }
    public void setSeasonCode(String seasonCode) { this.seasonName = seasonCode; }

    public com.mycropdiary.api.entity.Block getPlot() { return null; }
    public void setPlot(com.mycropdiary.api.entity.Block block) { }
}
