package com.mycropdiary.api.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Entity biểu diễn bảng dbo.FarmMember (Thành viên trang trại: OWNER hoặc STAFF).
 */
@Entity
@Table(name = "FarmMember", schema = "dbo", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"FarmID", "UserID"})
})
public class FarmMember extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FarmMemberID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FarmID", nullable = false)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "UserID", nullable = false)
    private AppUser user;

    @Column(name = "FarmRole", nullable = false, length = 20)
    private String farmRole; // OWNER, STAFF

    @Column(name = "JobTitle", length = 100)
    private String jobTitle;

    @Column(name = "JoinedAt", nullable = false)
    private LocalDate joinedAt = LocalDate.now();

    @Column(name = "LeftAt")
    private LocalDate leftAt;

    @Column(name = "Status", nullable = false, length = 20)
    private String status = "ACTIVE"; // INVITED, ACTIVE, INACTIVE

    public FarmMember() { }

    public FarmMember(Farm farm, AppUser user, String farmRole, String jobTitle, LocalDate joinedAt, String status) {
        this.farm = farm;
        this.user = user;
        this.farmRole = farmRole;
        this.jobTitle = jobTitle;
        if (joinedAt != null) {
            this.joinedAt = joinedAt;
        }
        if (status != null) {
            this.status = status;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public String getFarmRole() { return farmRole; }
    public void setFarmRole(String farmRole) { this.farmRole = farmRole; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public LocalDate getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDate joinedAt) { this.joinedAt = joinedAt; }

    public LocalDate getLeftAt() { return leftAt; }
    public void setLeftAt(LocalDate leftAt) { this.leftAt = leftAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
