package com.mycropdiary.api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity biểu diễn bảng dbo.Farm (Thông tin Trang trại).
 * Khớp chính xác 100% lược đồ dbo.Farm trong MyCropDiary_SQLServer.sql
 */
@Entity
@Table(name = "Farm", schema = "dbo")
public class Farm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FarmID")
    private Long id;

    @Column(name = "RegistrationID")
    private Long registrationId;

    @Column(name = "FarmCode", nullable = false, unique = true, length = 30)
    private String farmCode;

    @Column(name = "FarmName", nullable = false, length = 200)
    private String farmName;

    @Column(name = "AddressLine", length = 300)
    private String addressLine;

    @Column(name = "Province", length = 100)
    private String province;

    @Column(name = "District", length = 100)
    private String district;

    @Column(name = "Ward", length = 100)
    private String ward;

    @Column(name = "Latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "Longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "TotalAreaM2", precision = 18, scale = 2)
    private BigDecimal totalAreaM2;

    @Column(name = "Status", nullable = false, length = 20)
    private String status = "ACTIVE";

    public Farm() { }

    public Farm(String farmCode, String farmName, String addressLine, String province,
                String district, String ward, BigDecimal latitude, BigDecimal longitude,
                BigDecimal totalAreaM2, String status) {
        this.farmCode = farmCode;
        this.farmName = farmName;
        this.addressLine = addressLine;
        this.province = province;
        this.district = district;
        this.ward = ward;
        this.latitude = latitude;
        this.longitude = longitude;
        this.totalAreaM2 = totalAreaM2;
        if (status != null) {
            this.status = status;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRegistrationId() { return registrationId; }
    public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }

    public String getFarmCode() { return farmCode; }
    public void setFarmCode(String farmCode) { this.farmCode = farmCode; }

    public String getFarmName() { return farmName; }
    public void setFarmName(String farmName) { this.farmName = farmName; }

    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public BigDecimal getTotalAreaM2() { return totalAreaM2; }
    public void setTotalAreaM2(BigDecimal totalAreaM2) { this.totalAreaM2 = totalAreaM2; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

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
}
