package com.mycropdiary.api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity biểu diễn bảng dbo.ProductionArea (Vùng sản xuất thuộc Trang trại).
 * Khớp chính xác 100% lược đồ dbo.ProductionArea trong MyCropDiary_SQLServer.sql
 */
@Entity
@Table(name = "ProductionArea", schema = "dbo", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"FarmID", "AreaCode"})
})
public class ProductionArea {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProductionAreaID")
    private Long id;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return createdAt; }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FarmID", nullable = false)
    private Farm farm;

    @Column(name = "AreaCode", nullable = false, length = 30)
    private String areaCode;

    @Column(name = "AreaName", nullable = false, length = 150)
    private String areaName;

    @Column(name = "AreaM2", precision = 18, scale = 2)
    private BigDecimal areaM2;

    @Column(name = "Description", length = 1000)
    private String description;

    @Column(name = "Status", nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, INACTIVE

    public ProductionArea() { }

    public ProductionArea(Farm farm, String areaCode, String areaName, BigDecimal areaM2, String description, String status) {
        this.farm = farm;
        this.areaCode = areaCode;
        this.areaName = areaName;
        this.areaM2 = areaM2;
        this.description = description;
        if (status != null) {
            this.status = status;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }

    public String getAreaCode() { return areaCode; }
    public void setAreaCode(String areaCode) { this.areaCode = areaCode; }

    public String getAreaName() { return areaName; }
    public void setAreaName(String areaName) { this.areaName = areaName; }

    public BigDecimal getAreaM2() { return areaM2; }
    public void setAreaM2(BigDecimal areaM2) { this.areaM2 = areaM2; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
