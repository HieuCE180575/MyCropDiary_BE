package com.mycropdiary.api.entity.cultivation;

import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.Zone;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity biểu diễn bảng [InternalAssessment] (Phiếu đánh giá nội bộ theo tiêu chuẩn VietGAP).
 */
@Entity
@Table(name = "InternalAssessment")
public class InternalAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "internal_assessment_id", updatable = false, nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_season_id")
    private CropSeason cropSeason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessed_by")
    private FarmMember assessedBy;

    @Column(name = "assessment_title", nullable = false, length = 180)
    private String assessmentTitle;

    @Column(name = "assessed_at", nullable = false)
    private Instant assessedAt;

    @Column(name = "notes", columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    @Column(name = "evidence_image_url", columnDefinition = "NVARCHAR(MAX)")
    private String evidenceImageUrl;

    @OneToMany(mappedBy = "internalAssessment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InternalAssessmentItem> items = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (this.assessedAt == null) {
            this.assessedAt = Instant.now();
        }
    }

    public InternalAssessment() {
    }

    public InternalAssessment(Farm farm, CropSeason cropSeason, Zone zone, FarmMember assessedBy,
                              String assessmentTitle, Instant assessedAt, String notes, String evidenceImageUrl) {
        this.farm = farm;
        this.cropSeason = cropSeason;
        this.zone = zone;
        this.assessedBy = assessedBy;
        this.assessmentTitle = assessmentTitle;
        this.assessedAt = assessedAt != null ? assessedAt : Instant.now();
        this.notes = notes;
        this.evidenceImageUrl = evidenceImageUrl;
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

    public Zone getZone() {
        return zone;
    }

    public void setZone(Zone zone) {
        this.zone = zone;
    }

    public FarmMember getAssessedBy() {
        return assessedBy;
    }

    public void setAssessedBy(FarmMember assessedBy) {
        this.assessedBy = assessedBy;
    }

    public String getAssessmentTitle() {
        return assessmentTitle;
    }

    public void setAssessmentTitle(String assessmentTitle) {
        this.assessmentTitle = assessmentTitle;
    }

    public Instant getAssessedAt() {
        return assessedAt;
    }

    public void setAssessedAt(Instant assessedAt) {
        this.assessedAt = assessedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getEvidenceImageUrl() {
        return evidenceImageUrl;
    }

    public void setEvidenceImageUrl(String evidenceImageUrl) {
        this.evidenceImageUrl = evidenceImageUrl;
    }

    public List<InternalAssessmentItem> getItems() {
        return items;
    }

    public void setItems(List<InternalAssessmentItem> items) {
        this.items = items;
    }

    public void addItem(InternalAssessmentItem item) {
        this.items.add(item);
        item.setInternalAssessment(this);
    }
}

