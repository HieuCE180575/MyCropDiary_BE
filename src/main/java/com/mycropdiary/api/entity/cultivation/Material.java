package com.mycropdiary.api.entity.cultivation;

import com.mycropdiary.api.entity.Farm;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entity biểu diễn bảng [Material] (Vật tư nông nghiệp).
 * Khớp 100% với ERD mới.
 */
@Entity
@Table(name = "Material", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_Material_Name", columnNames = {"farm_id", "material_name"})
})
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "material_id", updatable = false, nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Column(name = "material_name", nullable = false, length = 160)
    private String materialName;

    @Column(name = "material_type", length = 80)
    private String materialType;

    @Column(name = "material_form", length = 60)
    private String materialForm;

    @Column(name = "unit", nullable = false, length = 30)
    private String unit;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "image_url", columnDefinition = "NVARCHAR(MAX)")
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Material() {}

    public Material(Farm farm, String materialName, String materialType, String unit) {
        this.farm = farm;
        this.materialName = materialName;
        this.materialType = materialType;
        this.unit = unit;
        this.isActive = true;
    }

    public Material(Farm farm, String materialName, String materialType, String materialForm, String unit) {
        this.farm = farm;
        this.materialName = materialName;
        this.materialType = materialType;
        this.materialForm = materialForm;
        this.unit = unit;
        this.isActive = true;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }

    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }

    public String getMaterialType() { return materialType; }
    public void setMaterialType(String materialType) { this.materialType = materialType; }

    public String getMaterialForm() { return materialForm; }
    public void setMaterialForm(String materialForm) { this.materialForm = materialForm; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    // --- Backward compatibility helpers ---
    public String getMaterialCode() { return materialName; }
    public void setMaterialCode(String materialCode) { this.materialName = materialCode; }
    public String getActiveIngredient() { return description; }
    public void setActiveIngredient(String activeIngredient) { this.description = activeIngredient; }
    public String getManufacturer() { return null; }
    public void setManufacturer(String manufacturer) { }
}
