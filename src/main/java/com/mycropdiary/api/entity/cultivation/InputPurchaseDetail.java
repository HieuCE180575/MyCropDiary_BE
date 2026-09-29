package com.mycropdiary.api.entity.cultivation;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity biểu diễn bảng dbo.InputPurchaseDetail (Chi tiết hóa đơn mua vật tư).
 */
@Entity
@Table(name = "InputPurchaseDetail", schema = "dbo")
public class InputPurchaseDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "InputPurchaseDetailID")
    private Long id;

    @Column(name = "InputPurchaseID", nullable = false)
    private Long inputPurchaseId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "MaterialID", nullable = false)
    private Material material;

    @Column(name = "BatchNumber", length = 80)
    private String batchNumber;

    @Column(name = "ManufactureDate")
    private LocalDate manufactureDate;

    @Column(name = "ExpiryDate")
    private LocalDate expiryDate;

    @Column(name = "Quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;

    @Column(name = "UnitPrice", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    public InputPurchaseDetail() {}

    public InputPurchaseDetail(Long id, Long inputPurchaseId, Material material, BigDecimal quantity, BigDecimal unitPrice) {
        this.id = id;
        this.inputPurchaseId = inputPurchaseId;
        this.material = material;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getInputPurchaseId() {
        return inputPurchaseId;
    }

    public void setInputPurchaseId(Long inputPurchaseId) {
        this.inputPurchaseId = inputPurchaseId;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public LocalDate getManufactureDate() {
        return manufactureDate;
    }

    public void setManufactureDate(LocalDate manufactureDate) {
        this.manufactureDate = manufactureDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
