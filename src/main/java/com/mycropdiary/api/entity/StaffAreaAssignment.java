package com.mycropdiary.api.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "StaffAreaAssignment", schema = "dbo")
public class StaffAreaAssignment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AssignmentID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FarmMemberID", nullable = false)
    private FarmMember farmMember;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ProductionAreaID", nullable = false)
    private ProductionArea productionArea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AssignedByMemberID")
    private FarmMember assignedByMember;

    @Column(name = "StartDate", nullable = false)
    private LocalDate startDate = LocalDate.now();

    @Column(name = "EndDate")
    private LocalDate endDate;

    @Column(name = "IsActive", nullable = false)
    private boolean active = true;

    public StaffAreaAssignment() { }

    public StaffAreaAssignment(FarmMember farmMember, ProductionArea productionArea,
                                FarmMember assignedByMember, LocalDate startDate, LocalDate endDate, boolean active) {
        this.farmMember = farmMember;
        this.productionArea = productionArea;
        this.assignedByMember = assignedByMember;
        if (startDate != null) {
            this.startDate = startDate;
        }
        this.endDate = endDate;
        this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public FarmMember getFarmMember() { return farmMember; }
    public void setFarmMember(FarmMember farmMember) { this.farmMember = farmMember; }

    public ProductionArea getProductionArea() { return productionArea; }
    public void setProductionArea(ProductionArea productionArea) { this.productionArea = productionArea; }

    public FarmMember getAssignedByMember() { return assignedByMember; }
    public void setAssignedByMember(FarmMember assignedByMember) { this.assignedByMember = assignedByMember; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
