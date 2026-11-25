package com.mycropdiary.api.entity.compliance;

import com.mycropdiary.api.entity.Farm;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Danh mục Thiết bị / Bình phun của Trang trại (UC-46 - Mục 4.9 VietGAP).
 */
@Entity
@Table(name = "FarmEquipment", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"farm_id", "equipment_code"})
})
public class FarmEquipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "equipment_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Column(name = "equipment_code", nullable = false, length = 60)
    private String equipmentCode;

    @Column(name = "equipment_name", nullable = false, length = 160)
    private String equipmentName;

    @Column(name = "equipment_type", nullable = false, length = 60)
    private String equipmentType = "sprayer";

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "notes", columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = Instant.now();
        if (this.status == null || this.status.isBlank()) this.status = "ACTIVE";
        if (this.equipmentType == null || this.equipmentType.isBlank()) this.equipmentType = "sprayer";
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }
    public String getEquipmentCode() { return equipmentCode; }
    public void setEquipmentCode(String equipmentCode) { this.equipmentCode = equipmentCode; }
    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }
    public String getEquipmentType() { return equipmentType; }
    public void setEquipmentType(String equipmentType) { this.equipmentType = equipmentType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
