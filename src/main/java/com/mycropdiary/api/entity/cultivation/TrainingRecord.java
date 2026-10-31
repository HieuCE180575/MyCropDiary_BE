package com.mycropdiary.api.entity.cultivation;

import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity biễu diễn bảng [TrainingRecord] (Hồ sơ đào tạo, tập huấn kỹ thuật VietGAP).
 */
@Entity
@Table(name = "TrainingRecord")
public class TrainingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "training_record_id", updatable = false, nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organized_by")
    private FarmMember organizedBy;

    @Column(name = "training_title", nullable = false, length = 180)
    private String trainingTitle;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "training_date", nullable = false)
    private LocalDate trainingDate;

    @Column(name = "image_url", columnDefinition = "NVARCHAR(MAX)")
    private String imageUrl;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "trainingRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrainingAttendee> attendees = new ArrayList<>();

    public TrainingRecord() {
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

    public FarmMember getOrganizedBy() {
        return organizedBy;
    }

    public void setOrganizedBy(FarmMember organizedBy) {
        this.organizedBy = organizedBy;
    }

    public String getTrainingTitle() {
        return trainingTitle;
    }

    public void setTrainingTitle(String trainingTitle) {
        this.trainingTitle = trainingTitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getTrainingDate() {
        return trainingDate;
    }

    public void setTrainingDate(LocalDate trainingDate) {
        this.trainingDate = trainingDate;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<TrainingAttendee> getAttendees() {
        return attendees;
    }

    public void setAttendees(List<TrainingAttendee> attendees) {
        this.attendees = attendees;
    }

    public void addAttendee(TrainingAttendee attendee) {
        this.attendees.add(attendee);
        attendee.setTrainingRecord(this);
    }

    public void removeAttendee(TrainingAttendee attendee) {
        this.attendees.remove(attendee);
        attendee.setTrainingRecord(null);
    }
}
