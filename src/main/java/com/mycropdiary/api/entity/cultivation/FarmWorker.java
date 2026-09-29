package com.mycropdiary.api.entity.cultivation;

import com.mycropdiary.api.entity.Farm;
import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Entity biểu diễn bảng dbo.FarmWorker (Hồ sơ công nhân trang trại).
 */
@Entity
@Table(name = "FarmWorker", schema = "dbo", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"FarmID", "WorkerCode"})
})
public class FarmWorker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FarmWorkerID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "FarmID", nullable = false)
    private Farm farm;

    @Column(name = "WorkerCode", nullable = false, length = 30)
    private String workerCode;

    @Column(name = "FullName", nullable = false, length = 150)
    private String fullName;

    @Column(name = "PhoneNumber", length = 20)
    private String phoneNumber;

    @Column(name = "DateOfBirth")
    private LocalDate dateOfBirth;

    @Column(name = "HireDate")
    private LocalDate hireDate;

    @Column(name = "Status", nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, INACTIVE

    @Column(name = "Notes", length = 1000)
    private String notes;

    public FarmWorker() { }

    public FarmWorker(Farm farm, String workerCode, String fullName, String phoneNumber, String status) {
        this.farm = farm;
        this.workerCode = workerCode;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        if (status != null) {
            this.status = status;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }

    public String getWorkerCode() { return workerCode; }
    public void setWorkerCode(String workerCode) { this.workerCode = workerCode; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
