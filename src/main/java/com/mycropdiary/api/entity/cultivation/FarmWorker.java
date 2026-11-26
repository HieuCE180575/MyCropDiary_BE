package com.mycropdiary.api.entity.cultivation;

import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Entity biểu diễn bảng [FarmWorker] (Hồ sơ công nhân lao động trang trại).
 * Hỗ trợ cả công nhân bên ngoài (không cần tài khoản hệ thống) và liên kết tùy chọn với FarmMember.
 */
@Entity
@Table(name = "FarmWorker")
public class FarmWorker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "farm_worker_id", updatable = false, nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id")
    private Farm farm;

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "farm_member_id", nullable = true)
    private FarmMember farmMember;

    @Column(name = "full_name", length = 120)
    private String fullName;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "worker_code", length = 40)
    private String workerCode;

    @Column(name = "job_title", length = 100)
    private String jobTitle;

    @Column(name = "employment_status", nullable = false, length = 20)
    private String employmentStatus = "active"; // active, inactive, seasonal

    @Column(name = "hired_at")
    private LocalDate hiredAt;

    @Column(name = "notes", columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    public FarmWorker() { }

    public FarmWorker(FarmMember farmMember, String workerCode, String jobTitle, String employmentStatus, LocalDate hiredAt, String notes) {
        this(farmMember != null ? farmMember.getFarm() : null, farmMember, null, null, workerCode, jobTitle, employmentStatus, hiredAt, notes);
    }

    public FarmWorker(Farm farm, FarmMember farmMember, String fullName, String phoneNumber, String workerCode, String jobTitle, String employmentStatus, LocalDate hiredAt, String notes) {
        this.farm = farm != null ? farm : (farmMember != null ? farmMember.getFarm() : null);
        this.farmMember = farmMember;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.workerCode = workerCode;
        this.jobTitle = jobTitle;
        if (employmentStatus != null) {
            this.employmentStatus = employmentStatus.toLowerCase();
        }
        this.hiredAt = hiredAt;
        this.notes = notes;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public void setFarm(Farm farm) { this.farm = farm; }

    public FarmMember getFarmMember() { return farmMember; }
    public void setFarmMember(FarmMember farmMember) { this.farmMember = farmMember; }

    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getWorkerCode() { return workerCode; }
    public void setWorkerCode(String workerCode) { this.workerCode = workerCode; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getEmploymentStatus() { return employmentStatus; }
    public void setEmploymentStatus(String employmentStatus) {
        this.employmentStatus = employmentStatus != null ? employmentStatus.toLowerCase() : "active";
    }

    public LocalDate getHiredAt() { return hiredAt; }
    public void setHiredAt(LocalDate hiredAt) { this.hiredAt = hiredAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    // --- Compatibility helper methods ---
    public Farm getFarm() {
        return farm != null ? farm : (farmMember != null ? farmMember.getFarm() : null);
    }

    public String getFullName() {
        if (fullName != null && !fullName.isBlank()) {
            return fullName;
        }
        return farmMember != null && farmMember.getUser() != null ? farmMember.getUser().getFullName() : null;
    }

    public String getPhoneNumber() {
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            return phoneNumber;
        }
        return farmMember != null && farmMember.getUser() != null ? farmMember.getUser().getPhone() : null;
    }

    public String getStatus() { return employmentStatus; }
    public void setStatus(String status) { setEmploymentStatus(status); }
}
