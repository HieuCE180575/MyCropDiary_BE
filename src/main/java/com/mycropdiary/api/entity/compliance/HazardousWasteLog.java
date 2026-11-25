package com.mycropdiary.api.entity.compliance;

import com.mycropdiary.api.entity.Farm;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Nhật ký Rác thải nguy hại & Vỏ bao bì thuốc BVTV / phân bón (UC-47 - Mục 4.8 VietGAP, BR-98).
 */
@Entity
@Table(name = "HazardousWasteLog")
public class HazardousWasteLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "waste_log_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Column(name = "waste_type", nullable = false, length = 50)
    private String wasteType; // 'pesticide_bottle', 'fertilizer_pack', 'medical', 'other'

    @Column(name = "quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Column(name = "collection_date", nullable = false)
    private LocalDate collectionDate;

    @Column(name = "disposal_method", nullable = false, length = 50)
    private String disposalMethod; // 'transfer_contractor', 'incinerate_farm', 'store_holding'

    @Column(name = "contractor_name", length = 160)
    private String contractorName;

    @Column(name = "handover_receipt_no", length = 80)
    private String handoverReceiptNo;

    @Column(name = "collector_name", nullable = false, length = 100)
    private String collectorName;

    @Column(name = "notes", columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = Instant.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }
    public String getWasteType() { return wasteType; }
    public void setWasteType(String wasteType) { this.wasteType = wasteType; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public LocalDate getCollectionDate() { return collectionDate; }
    public void setCollectionDate(LocalDate collectionDate) { this.collectionDate = collectionDate; }
    public String getDisposalMethod() { return disposalMethod; }
    public void setDisposalMethod(String disposalMethod) { this.disposalMethod = disposalMethod; }
    public String getContractorName() { return contractorName; }
    public void setContractorName(String contractorName) { this.contractorName = contractorName; }
    public String getHandoverReceiptNo() { return handoverReceiptNo; }
    public void setHandoverReceiptNo(String handoverReceiptNo) { this.handoverReceiptNo = handoverReceiptNo; }
    public String getCollectorName() { return collectorName; }
    public void setCollectorName(String collectorName) { this.collectorName = collectorName; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
