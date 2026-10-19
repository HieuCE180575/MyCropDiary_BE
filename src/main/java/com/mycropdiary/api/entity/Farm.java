package com.mycropdiary.api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Entity biểu diễn bảng [Farm] (Thông tin Trang trại).
 * Khớp 100% với ERD mới.
 */
@Entity
@Table(name = "Farm")
public class Farm extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "farm_id", updatable = false, nullable = false)
    private Integer id;

    @Column(name = "farm_name", nullable = false, length = 160)
    private String farmName;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "address", columnDefinition = "NVARCHAR(MAX)")
    private String address;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "cover_image_url", columnDefinition = "NVARCHAR(MAX)")
    private String coverImageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "farm_status", nullable = false, length = 20)
    private String farmStatus = "active";

    public Farm() { }

    public Farm(String farmName, String address, String description) {
        this.farmName = farmName;
        this.address = address;
        this.description = description;
        this.farmStatus = "active";
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getFarmName() { return farmName; }
    public void setFarmName(String farmName) { this.farmName = farmName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    // Alias for address
    public String getAddressLine() { return address; }
    public void setAddressLine(String addressLine) { this.address = addressLine; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public String getFarmStatus() { return farmStatus; }
    public void setFarmStatus(String farmStatus) {
        this.farmStatus = farmStatus != null ? farmStatus.toLowerCase() : "active";
    }

    // Alias for status
    public String getStatus() { return farmStatus; }
    public void setStatus(String status) { setFarmStatus(status); }

    public boolean isActive() { return "active".equalsIgnoreCase(farmStatus); }

    public String getFarmCode() { return id != null ? String.format("FARM-%04d", id) : ""; }
    public void setFarmCode(String farmCode) { }
    public String getProvince() { return null; }
    public void setProvince(String province) { }
    public String getDistrict() { return null; }
    public void setDistrict(String district) { }
    public String getWard() { return null; }
    public void setWard(String ward) { }
    public Integer getRegistrationId() { return null; }
    public void setRegistrationId(Integer regId) { }
}
