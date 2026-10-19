package com.mycropdiary.api.entity.cultivation;

import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity biểu diễn bảng [HarvestRecord] (Nhật ký thu hoạch sản phẩm).
 * Khớp 100% với ERD mới.
 */
@Entity
@Table(name = "HarvestRecord")
public class HarvestRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "harvest_record_id", updatable = false, nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_season_id")
    private CropSeason cropSeason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id")
    private Block block;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farming_activity_id")
    private FarmingActivity farmingActivity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private FarmMember recordedBy;

    @Column(name = "harvest_date", nullable = false)
    private LocalDate harvestDate;

    @Column(name = "quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit", nullable = false, length = 30)
    private String unit = "kg";

    @Column(name = "quality_grade", length = 50)
    private String qualityGrade;

    @Column(name = "image_url", columnDefinition = "NVARCHAR(MAX)")
    private String imageUrl;

    @Column(name = "notes", columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (harvestDate == null) {
            harvestDate = LocalDate.now();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public HarvestRecord() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }

    public CropSeason getCropSeason() { return cropSeason; }
    public void setCropSeason(CropSeason cropSeason) { this.cropSeason = cropSeason; }

    public Block getBlock() { return block; }
    public void setBlock(Block block) { this.block = block; }

    public FarmingActivity getFarmingActivity() { return farmingActivity; }
    public void setFarmingActivity(FarmingActivity farmingActivity) { this.farmingActivity = farmingActivity; }

    public FarmMember getRecordedBy() { return recordedBy; }
    public void setRecordedBy(FarmMember recordedBy) { this.recordedBy = recordedBy; }

    public LocalDate getHarvestDate() { return harvestDate; }
    public void setHarvestDate(LocalDate harvestDate) { this.harvestDate = harvestDate; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getQualityGrade() { return qualityGrade; }
    public void setQualityGrade(String qualityGrade) { this.qualityGrade = qualityGrade; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    // --- Backward compatibility helpers ---
    public FarmMember getRecordedByMember() { return recordedBy; }
    public void setRecordedByMember(FarmMember member) { this.recordedBy = member; }

    public LocalDateTime getHarvestedAt() {
        return harvestDate != null ? harvestDate.atStartOfDay() : null;
    }
    public void setHarvestedAt(LocalDateTime dt) {
        this.harvestDate = dt != null ? dt.toLocalDate() : null;
    }

    public String getHarvestLotCode() {
        return id != null ? String.format("LOT-%06d", id) : "LOT-PENDING";
    }
    public void setHarvestLotCode(String lot) { }

    public String getStorageLocation() { return null; }
    public void setStorageLocation(String loc) { }

    public String getTraceabilityCode() { return null; }
    public void setTraceabilityCode(String code) { }
}
