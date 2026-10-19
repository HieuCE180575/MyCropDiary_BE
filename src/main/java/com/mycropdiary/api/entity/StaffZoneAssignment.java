package com.mycropdiary.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entity biểu diễn bảng [StaffZoneAssignment] (Phân công nhân viên phụ trách Zone).
 * Khớp 100% lược đồ [StaffZoneAssignment] trong ERD mới.
 */
@Entity
@Table(name = "StaffZoneAssignment", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"farm_member_id", "zone_id", "assigned_at"})
})
public class StaffZoneAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id", updatable = false, nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_member_id", nullable = false)
    private FarmMember farmMember;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @PrePersist
    void onCreate() {
        if (assignedAt == null) {
            assignedAt = Instant.now();
        }
    }

    public StaffZoneAssignment() { }

    public StaffZoneAssignment(FarmMember farmMember, Zone zone) {
        this.farmMember = farmMember;
        this.zone = zone;
        this.assignedAt = Instant.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public FarmMember getFarmMember() { return farmMember; }
    public void setFarmMember(FarmMember farmMember) { this.farmMember = farmMember; }

    public Zone getZone() { return zone; }
    public void setZone(Zone zone) { this.zone = zone; }

    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }

    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }

    public boolean isActive() {
        return endedAt == null || endedAt.isAfter(Instant.now());
    }

    // --- Backward compatibility helpers for StaffAreaAssignment ---
    public Zone getProductionArea() { return zone; }
    public void setProductionArea(Zone zone) { this.zone = zone; }
}
