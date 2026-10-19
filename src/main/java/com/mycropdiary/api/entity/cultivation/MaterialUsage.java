package com.mycropdiary.api.entity.cultivation;

import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Entity biểu diễn bảng [MaterialUsage] (Nhật ký sử dụng vật tư).
 * Khớp 100% với ERD mới.
 */
@Entity
@Table(name = "MaterialUsage")
public class MaterialUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "material_usage_id", updatable = false, nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farming_activity_id")
    private FarmingActivity farmingActivity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "block_id")
    private Block block;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_by")
    private FarmMember usedBy;

    @Column(name = "quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(name = "used_at", nullable = false)
    private Instant usedAt;

    @Column(name = "notes", columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    @Column(name = "image_url", columnDefinition = "NVARCHAR(MAX)")
    private String imageUrl;

    @PrePersist
    void onCreate() {
        if (usedAt == null) {
            usedAt = Instant.now();
        }
    }

    public MaterialUsage() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }

    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }

    public FarmingActivity getFarmingActivity() { return farmingActivity; }
    public void setFarmingActivity(FarmingActivity farmingActivity) { this.farmingActivity = farmingActivity; }

    public Block getBlock() { return block; }
    public void setBlock(Block block) { this.block = block; }

    public FarmMember getUsedBy() { return usedBy; }
    public void setUsedBy(FarmMember usedBy) { this.usedBy = usedBy; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    // --- Backward compatibility helpers ---
    public FarmMember getRecordedByMember() { return usedBy; }
    public void setRecordedByMember(FarmMember recordedByMember) { this.usedBy = recordedByMember; }

    public String getUnit() { return material != null ? material.getUnit() : "kg"; }
    public void setUnit(String unit) { }

    public String getDosage() { return null; }
    public void setDosage(String dosage) { }

    public String getMethod() { return null; }
    public void setMethod(String method) { }

    public Integer getSafetyIntervalDays() { return null; }
    public void setSafetyIntervalDays(Integer safetyIntervalDays) { }

    public InputPurchaseDetail getInputPurchaseDetail() { return null; }
    public void setInputPurchaseDetail(InputPurchaseDetail inputPurchaseDetail) { }

    public LocalDateTime getUsedAtLdt() {
        return usedAt != null ? LocalDateTime.ofInstant(usedAt, ZoneId.systemDefault()) : null;
    }

    public void setUsedAt(LocalDateTime ldt) {
        if (ldt != null) {
            this.usedAt = ldt.atZone(ZoneId.systemDefault()).toInstant();
        }
    }

    public LocalDateTime getUsedAtLocalDateTime() {
        return getUsedAtLdt();
    }
    public void setUsedAtLocalDateTime(LocalDateTime ldt) {
        setUsedAt(ldt);
    }
}
