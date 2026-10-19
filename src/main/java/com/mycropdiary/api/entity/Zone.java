package com.mycropdiary.api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity biểu diễn bảng [Zone] (Khu vực / Phân khu sản xuất).
 * Khớp 100% lược đồ [Zone] trong ERD mới.
 */
@Entity
@Table(name = "Zone", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"farm_id", "zone_name"})
})
public class Zone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "zone_id", updatable = false, nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Column(name = "zone_name", nullable = false, length = 120)
    private String zoneName;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "image_url", columnDefinition = "NVARCHAR(MAX)")
    private String imageUrl;

    @Column(name = "area_value", precision = 12, scale = 2)
    private BigDecimal areaValue;

    @Column(name = "area_unit", length = 20)
    private String areaUnit = "m2";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Zone() { }

    public Zone(Farm farm, String zoneName, BigDecimal areaValue, String description) {
        this.farm = farm;
        this.zoneName = zoneName;
        this.areaValue = areaValue;
        this.description = description;
        this.areaUnit = "m2";
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }

    public String getZoneName() { return zoneName; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public BigDecimal getAreaValue() { return areaValue; }
    public void setAreaValue(BigDecimal areaValue) { this.areaValue = areaValue; }

    public String getAreaUnit() { return areaUnit; }
    public void setAreaUnit(String areaUnit) { this.areaUnit = areaUnit; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    // --- Backward compatibility helpers for ProductionArea ---
    public String getAreaName() { return zoneName; }
    public void setAreaName(String areaName) { this.zoneName = areaName; }

    public String getAreaCode() { return zoneName; }
    public void setAreaCode(String areaCode) { this.zoneName = areaCode; }

    public BigDecimal getAreaM2() { return areaValue; }
    public void setAreaM2(BigDecimal areaM2) { this.areaValue = areaM2; }
}
