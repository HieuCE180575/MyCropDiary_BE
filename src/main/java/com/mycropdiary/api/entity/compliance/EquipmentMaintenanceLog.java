package com.mycropdiary.api.entity.compliance;

import com.mycropdiary.api.entity.Farm;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Nhật ký Bảo dưỡng, Vệ sinh thiết bị / Bình phun (UC-46 - Mục 4.9 VietGAP, BR-99).
 */
@Entity
@Table(name = "EquipmentMaintenanceLog")
public class EquipmentMaintenanceLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maintenance_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id")
    private FarmEquipment equipment;

    @Column(name = "equipment_name", nullable = false, length = 160)
    private String equipmentName;

    @Column(name = "equipment_code", length = 60)
    private String equipmentCode;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType; // 'rinse_cleaning', 'maintenance_repair', 'calibration'

    @Column(name = "action_date", nullable = false)
    private LocalDate actionDate;

    @Column(name = "performer_name", nullable = false, length = 100)
    private String performerName;

    @Column(name = "rinse_water_handling", nullable = false, length = 50)
    private String rinseWaterHandling; // 'holding_tank', 'diluted_field', 'special_pit'

    @Column(name = "is_safe_for_use", nullable = false)
    private Boolean isSafeForUse = true;

    @Column(name = "notes", columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = Instant.now();
        if (this.isSafeForUse == null) this.isSafeForUse = true;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }
    public FarmEquipment getEquipment() { return equipment; }
    public void setEquipment(FarmEquipment equipment) { this.equipment = equipment; }
    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }
    public String getEquipmentCode() { return equipmentCode; }
    public void setEquipmentCode(String equipmentCode) { this.equipmentCode = equipmentCode; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public LocalDate getActionDate() { return actionDate; }
    public void setActionDate(LocalDate actionDate) { this.actionDate = actionDate; }
    public String getPerformerName() { return performerName; }
    public void setPerformerName(String performerName) { this.performerName = performerName; }
    public String getRinseWaterHandling() { return rinseWaterHandling; }
    public void setRinseWaterHandling(String rinseWaterHandling) { this.rinseWaterHandling = rinseWaterHandling; }
    public Boolean getIsSafeForUse() { return isSafeForUse; }
    public void setIsSafeForUse(Boolean isSafeForUse) { this.isSafeForUse = isSafeForUse; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
