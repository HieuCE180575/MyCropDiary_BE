package com.mycropdiary.api.entity.cultivation;

import com.mycropdiary.api.entity.FarmMember;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity biểu diễn bảng dbo.HarvestRecord (Nhật ký thu hoạch sản phẩm).
 */
@Entity
@Table(name = "HarvestRecord", schema = "dbo", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_HarvestRecord_Lot", columnNames = {"HarvestLotCode"})
})
public class HarvestRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HarvestRecordID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CropSeasonID", nullable = false)
    private CropSeason cropSeason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RecordedByMemberID", nullable = false)
    private FarmMember recordedByMember;

    @Column(name = "HarvestLotCode", nullable = false, length = 60, unique = true)
    private String harvestLotCode;

    @Column(name = "HarvestedAt", nullable = false)
    private LocalDateTime harvestedAt;

    @Column(name = "Quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;

    @Column(name = "Unit", nullable = false, length = 30)
    private String unit;

    @Column(name = "QualityGrade", length = 50)
    private String qualityGrade;

    @Column(name = "StorageLocation", length = 200)
    private String storageLocation;

    @Column(name = "TraceabilityCode", length = 100)
    private String traceabilityCode;

    @Column(name = "Notes", length = 1000)
    private String notes;

    public HarvestRecord() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CropSeason getCropSeason() {
        return cropSeason;
    }

    public void setCropSeason(CropSeason cropSeason) {
        this.cropSeason = cropSeason;
    }

    public FarmMember getRecordedByMember() {
        return recordedByMember;
    }

    public void setRecordedByMember(FarmMember recordedByMember) {
        this.recordedByMember = recordedByMember;
    }

    public String getHarvestLotCode() {
        return harvestLotCode;
    }

    public void setHarvestLotCode(String harvestLotCode) {
        this.harvestLotCode = harvestLotCode;
    }

    public LocalDateTime getHarvestedAt() {
        return harvestedAt;
    }

    public void setHarvestedAt(LocalDateTime harvestedAt) {
        this.harvestedAt = harvestedAt;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getQualityGrade() {
        return qualityGrade;
    }

    public void setQualityGrade(String qualityGrade) {
        this.qualityGrade = qualityGrade;
    }

    public String getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
    }

    public String getTraceabilityCode() {
        return traceabilityCode;
    }

    public void setTraceabilityCode(String traceabilityCode) {
        this.traceabilityCode = traceabilityCode;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
