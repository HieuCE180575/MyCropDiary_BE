package com.mycropdiary.api.entity.cultivation;

import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Entity biểu diễn bảng [Expense] (Chi phí sản xuất và chi phí vận hành trang trại).
 */
@Entity
@Table(name = "Expense")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_id", updatable = false, nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_season_id")
    private CropSeason cropSeason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_task_id")
    private FarmTask farmTask;

    @Column(name = "expense_category", nullable = false, length = 80)
    private String expenseCategory;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private FarmMember recordedBy;

    @Column(name = "receipt_image_url", columnDefinition = "NVARCHAR(MAX)")
    private String receiptImageUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (this.expenseDate == null) {
            this.expenseDate = LocalDate.now();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public Expense() {
    }

    public Expense(Farm farm, CropSeason cropSeason, FarmTask farmTask, String expenseCategory,
                   String description, BigDecimal amount, LocalDate expenseDate,
                   FarmMember recordedBy, String receiptImageUrl) {
        this.farm = farm;
        this.cropSeason = cropSeason;
        this.farmTask = farmTask;
        this.expenseCategory = expenseCategory;
        this.description = description;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.expenseDate = expenseDate != null ? expenseDate : LocalDate.now();
        this.recordedBy = recordedBy;
        this.receiptImageUrl = receiptImageUrl;
        this.createdAt = Instant.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Farm getFarm() {
        return farm;
    }

    public void setFarm(Farm farm) {
        this.farm = farm;
    }

    public CropSeason getCropSeason() {
        return cropSeason;
    }

    public void setCropSeason(CropSeason cropSeason) {
        this.cropSeason = cropSeason;
    }

    public FarmTask getFarmTask() {
        return farmTask;
    }

    public void setFarmTask(FarmTask farmTask) {
        this.farmTask = farmTask;
    }

    public String getExpenseCategory() {
        return expenseCategory;
    }

    public void setExpenseCategory(String expenseCategory) {
        this.expenseCategory = expenseCategory;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public FarmMember getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(FarmMember recordedBy) {
        this.recordedBy = recordedBy;
    }

    public String getReceiptImageUrl() {
        return receiptImageUrl;
    }

    public void setReceiptImageUrl(String receiptImageUrl) {
        this.receiptImageUrl = receiptImageUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

