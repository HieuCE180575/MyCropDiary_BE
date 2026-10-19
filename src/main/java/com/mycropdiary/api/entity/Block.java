package com.mycropdiary.api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity biểu diễn bảng [Block] (Lô / Thửa đất sản xuất trong Zone).
 * Khớp 100% lược đồ [Block] trong ERD mới.
 */
@Entity
@Table(name = "Block", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"zone_id", "block_name"})
})
public class Block {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "block_id", updatable = false, nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    @Column(name = "block_name", nullable = false, length = 120)
    private String blockName;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "image_url", columnDefinition = "NVARCHAR(MAX)")
    private String imageUrl;

    @Column(name = "area_value", precision = 12, scale = 2)
    private BigDecimal areaValue;

    @Column(name = "area_unit", length = 20)
    private String areaUnit = "m2";

    @Column(name = "soil_type", length = 100)
    private String soilType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Block() { }

    public Block(Zone zone, String blockName, BigDecimal areaValue, String description) {
        this.zone = zone;
        this.blockName = blockName;
        this.areaValue = areaValue;
        this.description = description;
        this.areaUnit = "m2";
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Zone getZone() { return zone; }
    public void setZone(Zone zone) { this.zone = zone; }

    public String getBlockName() { return blockName; }
    public void setBlockName(String blockName) { this.blockName = blockName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public BigDecimal getAreaValue() { return areaValue; }
    public void setAreaValue(BigDecimal areaValue) { this.areaValue = areaValue; }

    public String getAreaUnit() { return areaUnit; }
    public void setAreaUnit(String areaUnit) { this.areaUnit = areaUnit; }

    public String getSoilType() { return soilType; }
    public void setSoilType(String soilType) { this.soilType = soilType; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    // --- Backward compatibility helpers for Plot / ProductionArea ---
    public Zone getProductionArea() { return zone; }
    public void setProductionArea(Zone zone) { this.zone = zone; }

    public String getPlotName() { return blockName; }
    public void setPlotName(String plotName) { this.blockName = plotName; }

    public String getPlotCode() { return blockName; }
    public void setPlotCode(String plotCode) { this.blockName = plotCode; }

    public BigDecimal getAreaM2() { return areaValue; }
    public void setAreaM2(BigDecimal areaM2) { this.areaValue = areaM2; }
}
