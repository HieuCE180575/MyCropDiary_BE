package com.mycropdiary.api.entity.cultivation;

import com.mycropdiary.api.entity.Farm;
import jakarta.persistence.*;

/**
 * Entity biểu diễn bảng dbo.Material (Vật tư nông nghiệp).
 */
@Entity
@Table(name = "Material", schema = "dbo", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_Material_Code", columnNames = {"FarmID", "MaterialCode"})
})
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaterialID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FarmID", nullable = false)
    private Farm farm;

    @Column(name = "MaterialCode", nullable = false, length = 30)
    private String materialCode;

    @Column(name = "MaterialName", nullable = false, length = 200)
    private String materialName;

    @Enumerated(EnumType.STRING)
    @Column(name = "MaterialType", nullable = false, length = 30)
    private MaterialType materialType;

    @Column(name = "Unit", nullable = false, length = 30)
    private String unit;

    @Column(name = "ActiveIngredient", length = 300)
    private String activeIngredient;

    @Column(name = "Manufacturer", length = 200)
    private String manufacturer;

    @Column(name = "IsActive", nullable = false)
    private Boolean isActive = true;

    public Material() {}

    public Material(Long id, Farm farm, String materialCode, String materialName, MaterialType materialType, String unit, Boolean isActive) {
        this.id = id;
        this.farm = farm;
        this.materialCode = materialCode;
        this.materialName = materialName;
        this.materialType = materialType;
        this.unit = unit;
        this.isActive = isActive;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Farm getFarm() {
        return farm;
    }

    public void setFarm(Farm farm) {
        this.farm = farm;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public MaterialType getMaterialType() {
        return materialType;
    }

    public void setMaterialType(MaterialType materialType) {
        this.materialType = materialType;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getActiveIngredient() {
        return activeIngredient;
    }

    public void setActiveIngredient(String activeIngredient) {
        this.activeIngredient = activeIngredient;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
