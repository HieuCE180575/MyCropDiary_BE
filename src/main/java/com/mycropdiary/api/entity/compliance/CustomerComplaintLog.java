package com.mycropdiary.api.entity.compliance;

import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.cultivation.HarvestRecord;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Nhật ký Khiếu nại khách hàng & Thu hồi sản phẩm (UC-48 - Mục 4.10 VietGAP, BR-100).
 */
@Entity
@Table(name = "CustomerComplaintLog")
public class CustomerComplaintLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "complaint_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "harvest_record_id")
    private HarvestRecord harvestRecord;

    @Column(name = "lot_code", length = 80)
    private String lotCode;

    @Column(name = "customer_name", nullable = false, length = 160)
    private String customerName;

    @Column(name = "customer_phone", length = 30)
    private String customerPhone;

    @Column(name = "received_date", nullable = false)
    private LocalDate receivedDate;

    @Column(name = "issue_type", nullable = false, length = 50)
    private String issueType; // 'quality_defect', 'chemical_residue_suspect', 'packaging_damage', 'other'

    @Column(name = "issue_description", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String issueDescription;

    @Column(name = "severity", nullable = false, length = 30)
    private String severity; // 'low', 'medium', 'critical'

    @Column(name = "status", nullable = false, length = 30)
    private String status = "received"; // 'received', 'investigating', 'resolved', 'recalled'

    @Column(name = "resolution_notes", columnDefinition = "NVARCHAR(MAX)")
    private String resolutionNotes;

    @Column(name = "is_recall_required", nullable = false)
    private Boolean isRecallRequired = false;

    @Column(name = "recalled_quantity", precision = 12, scale = 2)
    private BigDecimal recalledQuantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = Instant.now();
        if (this.status == null || this.status.isBlank()) this.status = "received";
        if (this.isRecallRequired == null) this.isRecallRequired = false;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }
    public HarvestRecord getHarvestRecord() { return harvestRecord; }
    public void setHarvestRecord(HarvestRecord harvestRecord) { this.harvestRecord = harvestRecord; }
    public String getLotCode() { return lotCode; }
    public void setLotCode(String lotCode) { this.lotCode = lotCode; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public LocalDate getReceivedDate() { return receivedDate; }
    public void setReceivedDate(LocalDate receivedDate) { this.receivedDate = receivedDate; }
    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }
    public String getIssueDescription() { return issueDescription; }
    public void setIssueDescription(String issueDescription) { this.issueDescription = issueDescription; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    public Boolean getIsRecallRequired() { return isRecallRequired; }
    public void setIsRecallRequired(Boolean isRecallRequired) { this.isRecallRequired = isRecallRequired; }
    public BigDecimal getRecalledQuantity() { return recalledQuantity; }
    public void setRecalledQuantity(BigDecimal recalledQuantity) { this.recalledQuantity = recalledQuantity; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
