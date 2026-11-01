package com.mycropdiary.api.entity.cultivation;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Entity biểu diễn bảng [InternalAssessmentItem] (Chi tiết điểm số từng tiêu chí đánh giá nội bộ).
 */
@Entity
@Table(name = "InternalAssessmentItem", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"internal_assessment_id", "assessment_criterion_id"})
})
public class InternalAssessmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "internal_assessment_item_id", updatable = false, nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "internal_assessment_id", nullable = false)
    private InternalAssessment internalAssessment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_criterion_id", nullable = false)
    private AssessmentCriterion assessmentCriterion;

    @Column(name = "score", nullable = false, precision = 8, scale = 2)
    private BigDecimal score;

    @Column(name = "result_notes", columnDefinition = "NVARCHAR(MAX)")
    private String resultNotes;

    @Column(name = "evidence_image_url", columnDefinition = "NVARCHAR(MAX)")
    private String evidenceImageUrl;

    public InternalAssessmentItem() {
    }

    public InternalAssessmentItem(InternalAssessment internalAssessment, AssessmentCriterion assessmentCriterion,
                                  BigDecimal score, String resultNotes, String evidenceImageUrl) {
        this.internalAssessment = internalAssessment;
        this.assessmentCriterion = assessmentCriterion;
        this.score = score != null ? score : BigDecimal.ZERO;
        this.resultNotes = resultNotes;
        this.evidenceImageUrl = evidenceImageUrl;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public InternalAssessment getInternalAssessment() {
        return internalAssessment;
    }

    public void setInternalAssessment(InternalAssessment internalAssessment) {
        this.internalAssessment = internalAssessment;
    }

    public AssessmentCriterion getAssessmentCriterion() {
        return assessmentCriterion;
    }

    public void setAssessmentCriterion(AssessmentCriterion assessmentCriterion) {
        this.assessmentCriterion = assessmentCriterion;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public String getResultNotes() {
        return resultNotes;
    }

    public void setResultNotes(String resultNotes) {
        this.resultNotes = resultNotes;
    }

    public String getEvidenceImageUrl() {
        return evidenceImageUrl;
    }

    public void setEvidenceImageUrl(String evidenceImageUrl) {
        this.evidenceImageUrl = evidenceImageUrl;
    }
}

