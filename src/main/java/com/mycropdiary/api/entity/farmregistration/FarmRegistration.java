package com.mycropdiary.api.entity.farmregistration;

import com.mycropdiary.api.entity.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "FarmRegistration")
public class FarmRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "registration_id", updatable = false, nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "farm_name", nullable = false, length = 160)
    private String farmName;

    @Column(name = "address", columnDefinition = "NVARCHAR(MAX)")
    private String address;

    @Column(name = "province", length = 100)
    private String province;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "ward", length = 100)
    private String ward;

    @Column(name = "document_url", length = 1000)
    private String documentUrl;

    @Column(name = "registration_note", columnDefinition = "NVARCHAR(MAX)")
    private String registrationNote;

    @Column(name = "registration_status", nullable = false, length = 20)
    private String registrationStatus = "pending"; // pending, approved, rejected, cancelled

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "resulting_farm_id")
    private Integer resultingFarmId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public FarmRegistration() { }

    public FarmRegistration(User user, String farmName, String address) {
        this.user = user;
        this.farmName = farmName;
        this.address = address;
        this.registrationStatus = "pending";
    }

    public FarmRegistration(User user, String farmName, String address, String province, String district, String ward, String documentUrl, String registrationNote) {
        this.user = user;
        this.farmName = farmName;
        this.address = address;
        this.province = province;
        this.district = district;
        this.ward = ward;
        this.documentUrl = documentUrl;
        this.registrationNote = registrationNote;
        this.registrationStatus = "pending";
    }

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getFarmName() { return farmName; }
    public void setFarmName(String farmName) { this.farmName = farmName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getRegistrationNote() { return registrationNote; }
    public void setRegistrationNote(String registrationNote) { this.registrationNote = registrationNote; }

    public String getRegistrationStatus() { return registrationStatus; }
    public void setRegistrationStatus(String registrationStatus) {
        this.registrationStatus = registrationStatus != null ? registrationStatus.toLowerCase() : "pending";
    }

    public User getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(User reviewedBy) { this.reviewedBy = reviewedBy; }

    public Instant getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }

    public Integer getResultingFarmId() { return resultingFarmId; }
    public void setResultingFarmId(Integer resultingFarmId) { this.resultingFarmId = resultingFarmId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    // --- Compatibility Helpers ---
    public User getApplicantUser() { return user; }
    public void setApplicantUser(User user) { this.user = user; }

    public User getHandlerUser() { return reviewedBy; }
    public void setHandlerUser(User handlerUser) { this.reviewedBy = handlerUser; }

    public java.time.LocalDateTime getHandledAt() {
        return reviewedAt != null ? java.time.LocalDateTime.ofInstant(reviewedAt, java.time.ZoneId.systemDefault()) : null;
    }
    public void setHandledAt(java.time.LocalDateTime handledAt) {
        this.reviewedAt = handledAt != null ? handledAt.atZone(java.time.ZoneId.systemDefault()).toInstant() : null;
    }
    public void setHandledAt(Instant handledAt) { this.reviewedAt = handledAt; }

    public java.time.LocalDateTime getSubmittedAt() {
        return createdAt != null ? java.time.LocalDateTime.ofInstant(createdAt, java.time.ZoneId.systemDefault()) : null;
    }
    public void setSubmittedAt(java.time.LocalDateTime submittedAt) {
        this.createdAt = submittedAt != null ? submittedAt.atZone(java.time.ZoneId.systemDefault()).toInstant() : null;
    }
    public void setSubmittedAt(Instant submittedAt) { this.createdAt = submittedAt; }

    public String getAddressLine() { return address; }
    public void setAddressLine(String addressLine) { this.address = addressLine; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public String getDocumentUrl() { return documentUrl; }
    public void setDocumentUrl(String documentUrl) { this.documentUrl = documentUrl; }

    public String getDescription() { return registrationNote; }
    public void setDescription(String description) { this.registrationNote = description; }

    public String getRejectionReason() { return registrationNote; }
    public void setRejectionReason(String rejectionReason) { this.registrationNote = rejectionReason; }

    public FarmRegistrationStatus getStatus() {
        if ("approved".equalsIgnoreCase(registrationStatus)) return FarmRegistrationStatus.APPROVED;
        if ("rejected".equalsIgnoreCase(registrationStatus)) return FarmRegistrationStatus.REJECTED;
        if ("cancelled".equalsIgnoreCase(registrationStatus)) return FarmRegistrationStatus.CANCELLED;
        return FarmRegistrationStatus.PENDING;
    }

    public void setStatus(FarmRegistrationStatus status) {
        this.registrationStatus = status != null ? status.name().toLowerCase() : "pending";
    }
}
