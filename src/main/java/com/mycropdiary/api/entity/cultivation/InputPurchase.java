package com.mycropdiary.api.entity.cultivation;

import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity biểu diễn bảng [InputPurchase] (Hóa đơn / Phiếu mua vật tư nông nghiệp).
 * Khớp 100% với lược đồ database SQL Server [InputPurchase].
 */
@Entity
@Table(name = "InputPurchase")
public class InputPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "input_purchase_id", updatable = false, nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchased_by")
    private FarmMember purchasedBy;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name = "reference_no", length = 80)
    private String referenceNo;

    @Column(name = "total_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "receipt_image_url", columnDefinition = "NVARCHAR(MAX)")
    private String receiptImageUrl;

    @Column(name = "warehouse_receipt_no", length = 80)
    private String warehouseReceiptNo;

    @Column(name = "notes", columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "inputPurchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InputPurchaseItem> items = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (purchaseDate == null) {
            purchaseDate = LocalDate.now();
        }
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }
    }

    public InputPurchase() { }

    public InputPurchase(Farm farm, Supplier supplier, FarmMember purchasedBy, LocalDate purchaseDate,
                         String referenceNo, BigDecimal totalAmount, String receiptImageUrl, String notes) {
        this.farm = farm;
        this.supplier = supplier;
        this.purchasedBy = purchasedBy;
        this.purchaseDate = purchaseDate != null ? purchaseDate : LocalDate.now();
        this.referenceNo = referenceNo;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        this.receiptImageUrl = receiptImageUrl;
        this.notes = notes;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }

    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }

    public FarmMember getPurchasedBy() { return purchasedBy; }
    public void setPurchasedBy(FarmMember purchasedBy) { this.purchasedBy = purchasedBy; }

    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }

    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getReceiptImageUrl() { return receiptImageUrl; }
    public void setReceiptImageUrl(String receiptImageUrl) { this.receiptImageUrl = receiptImageUrl; }

    public String getWarehouseReceiptNo() { return warehouseReceiptNo; }
    public void setWarehouseReceiptNo(String warehouseReceiptNo) { this.warehouseReceiptNo = warehouseReceiptNo; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public List<InputPurchaseItem> getItems() { return items; }
    public void setItems(List<InputPurchaseItem> items) { this.items = items; }

    public void addItem(InputPurchaseItem item) {
        items.add(item);
        item.setInputPurchase(this);
    }
}

