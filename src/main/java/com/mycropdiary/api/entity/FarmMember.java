package com.mycropdiary.api.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity biểu diễn bảng [FarmMember] (Thành viên trang trại).
 * Khớp 100% với ERD mới: member_role in ('owner','admin','manager','member'),
 * membership_status in ('invited','active','suspended','left').
 */
@Entity
@Table(name = "FarmMember", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"farm_id", "user_id"})
})
public class FarmMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "farm_member_id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "member_role", nullable = false, length = 30)
    private String memberRole = "member"; // owner, admin, manager, member

    @Column(name = "membership_status", nullable = false, length = 20)
    private String membershipStatus = "active"; // invited, active, suspended, left

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @PrePersist
    void onCreate() {
        if (joinedAt == null) {
            joinedAt = Instant.now();
        }
    }

    public FarmMember() { }

    public FarmMember(Farm farm, User user, String memberRole, String membershipStatus) {
        this.farm = farm;
        this.user = user;
        if (memberRole != null) {
            this.memberRole = memberRole.toLowerCase();
        }
        if (membershipStatus != null) {
            this.membershipStatus = membershipStatus.toLowerCase();
        }
        this.joinedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getMemberRole() { return memberRole; }
    public void setMemberRole(String memberRole) {
        this.memberRole = memberRole != null ? memberRole.toLowerCase() : "member";
    }

    // Alias for farmRole
    public String getFarmRole() { return memberRole; }
    public void setFarmRole(String farmRole) { setMemberRole(farmRole); }

    public String getMembershipStatus() { return membershipStatus; }
    public void setMembershipStatus(String membershipStatus) {
        this.membershipStatus = membershipStatus != null ? membershipStatus.toLowerCase() : "active";
    }

    // Alias for status
    public String getStatus() { return membershipStatus; }
    public void setStatus(String status) { setMembershipStatus(status); }

    public Instant getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Instant joinedAt) { this.joinedAt = joinedAt; }

    public boolean isOwner() {
        return "owner".equalsIgnoreCase(memberRole);
    }

    public boolean isActive() {
        return "active".equalsIgnoreCase(membershipStatus);
    }

    // Compatibility helpers
    @Transient
    private String jobTitle;

    public String getJobTitle() {
        return jobTitle != null ? jobTitle : memberRole;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }
}
