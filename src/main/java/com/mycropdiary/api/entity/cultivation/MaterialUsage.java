package com.mycropdiary.api.entity.cultivation;

import com.mycropdiary.api.entity.FarmMember;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity biểu diễn bảng dbo.MaterialUsage (Nhật ký sử dụng vật tư).
 */
@Entity
@Table(name = "MaterialUsage", schema = "dbo")
public class MaterialUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaterialUsageID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FarmingActivityID", nullable = false)
    private FarmingActivity farmingActivity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "InputPurchaseDetailID")
    private InputPurchaseDetail inputPurchaseDetail;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MaterialID", nullable = false)
    private Material material;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RecordedByMemberID", nullable = false)
    private FarmMember recordedByMember;

    @Column(name = "UsedAt", nullable = false)
    private LocalDateTime usedAt;

    @Column(name = "Quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;

    @Column(name = "Unit", nullable = false, length = 30)
    private String unit;

    @Column(name = "Dosage", length = 100)
    private String dosage;

    @Column(name = "Method", length = 200)
    private String method;

    @Column(name = "SafetyIntervalDays")
    private Integer safetyIntervalDays;

    @Column(name = "Notes", length = 1000)
    private String notes;

    public MaterialUsage() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FarmingActivity getFarmingActivity() {
        return farmingActivity;
    }

    public void setFarmingActivity(FarmingActivity farmingActivity) {
        this.farmingActivity = farmingActivity;
    }

    public InputPurchaseDetail getInputPurchaseDetail() {
        return inputPurchaseDetail;
    }

    public void setInputPurchaseDetail(InputPurchaseDetail inputPurchaseDetail) {
        this.inputPurchaseDetail = inputPurchaseDetail;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public FarmMember getRecordedByMember() {
        return recordedByMember;
    }

    public void setRecordedByMember(FarmMember recordedByMember) {
        this.recordedByMember = recordedByMember;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
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

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Integer getSafetyIntervalDays() {
        return safetyIntervalDays;
    }

    public void setSafetyIntervalDays(Integer safetyIntervalDays) {
        this.safetyIntervalDays = safetyIntervalDays;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
