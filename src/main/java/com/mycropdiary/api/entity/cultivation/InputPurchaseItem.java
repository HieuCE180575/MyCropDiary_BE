package com.mycropdiary.api.entity.cultivation;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Entity biểu diễn bảng [InputPurchaseItem] (Chi tiết mặt hàng trong phiếu mua vật tư).
 * Khớp 100% với lược đồ database SQL Server [InputPurchaseItem].
 */
@Entity
@Table(name = "InputPurchaseItem")
public class InputPurchaseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "purchase_item_id", updatable = false, nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "input_purchase_id", nullable = false)
    private InputPurchase inputPurchase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "warehouse_id")
    private Integer warehouseId;

    @Column(name = "quantity", precision = 14, scale = 3, nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_cost", precision = 14, scale = 2, nullable = false)
    private BigDecimal unitCost;

    @Column(name = "line_total", precision = 14, scale = 2, insertable = false, updatable = false)
    private BigDecimal lineTotal;

    public InputPurchaseItem() { }

    public InputPurchaseItem(InputPurchase inputPurchase, Material material, Integer warehouseId, BigDecimal quantity, BigDecimal unitCost) {
        this.inputPurchase = inputPurchase;
        this.material = material;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
        this.unitCost = unitCost;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public InputPurchase getInputPurchase() { return inputPurchase; }
    public void setInputPurchase(InputPurchase inputPurchase) { this.inputPurchase = inputPurchase; }

    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }

    public Integer getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Integer warehouseId) { this.warehouseId = warehouseId; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public BigDecimal getLineTotal() {
        if (lineTotal != null) return lineTotal;
        if (quantity != null && unitCost != null) {
            return quantity.multiply(unitCost);
        }
        return BigDecimal.ZERO;
    }

    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
}
