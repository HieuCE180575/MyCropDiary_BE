package com.mycropdiary.api.entity.farmregistration;

import com.mycropdiary.api.entity.AppUser;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "FarmRegistration", schema = "dbo")
public class FarmRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RegistrationID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ApplicantUserID", nullable = false)
    private AppUser applicantUser;

    @Column(name = "FarmName", nullable = false, length = 200)
    private String farmName;

    @Column(name = "AddressLine", nullable = false, length = 300)
    private String addressLine;

    @Column(name = "Province", length = 100)
    private String province;

    @Column(name = "District", length = 100)
    private String district;

    @Column(name = "Ward", length = 100)
    private String ward;

    @Column(name = "Description", length = 1000)
    private String description;

    @Column(name = "DocumentUrl", length = 1000)
    private String documentUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 20)
    private FarmRegistrationStatus status = FarmRegistrationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HandlerUserID")
    private AppUser handlerUser;

    @Column(name = "SubmittedAt", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "HandledAt")
    private LocalDateTime handledAt;

    @Column(name = "RejectionReason", length = 1000)
    private String rejectionReason;

    public FarmRegistration() {
    }

    public FarmRegistration(AppUser applicantUser, String farmName, String addressLine) {
        this.applicantUser = applicantUser;
        this.farmName = farmName;
        this.addressLine = addressLine;
    }

    @PrePersist
    void onCreate() {
        if (this.submittedAt == null) {
            this.submittedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppUser getApplicantUser() {
        return applicantUser;
    }

    public void setApplicantUser(AppUser applicantUser) {
        this.applicantUser = applicantUser;
    }

    public String getFarmName() {
        return farmName;
    }

    public void setFarmName(String farmName) {
        this.farmName = farmName;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }

    public FarmRegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(FarmRegistrationStatus status) {
        this.status = status;
    }

    public AppUser getHandlerUser() {
        return handlerUser;
    }

    public void setHandlerUser(AppUser handlerUser) {
        this.handlerUser = handlerUser;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public void setHandledAt(LocalDateTime handledAt) {
        this.handledAt = handledAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
