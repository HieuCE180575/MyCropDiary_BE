package com.mycropdiary.api.entity.cultivation;

import jakarta.persistence.*;

/**
 * Entity biễu diễn bảng [TrainingAttendee] (Danh sách công nhân tham gia khóa đào tạo).
 */
@Entity
@Table(name = "TrainingAttendee")
public class TrainingAttendee {

    @EmbeddedId
    private TrainingAttendeeId id = new TrainingAttendeeId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("trainingRecordId")
    @JoinColumn(name = "training_record_id", nullable = false)
    private TrainingRecord trainingRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("farmWorkerId")
    @JoinColumn(name = "farm_worker_id", nullable = false)
    private FarmWorker farmWorker;

    @Column(name = "attendance_status", nullable = false, length = 20)
    private String attendanceStatus = "attended"; // attended, absent, excused

    @Column(name = "notes", columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    public TrainingAttendee() {
    }

    public TrainingAttendee(TrainingRecord trainingRecord, FarmWorker farmWorker, String attendanceStatus, String notes) {
        this.trainingRecord = trainingRecord;
        this.farmWorker = farmWorker;
        this.attendanceStatus = attendanceStatus != null ? attendanceStatus : "attended";
        this.notes = notes;
        this.id = new TrainingAttendeeId(
                trainingRecord != null ? trainingRecord.getId() : null,
                farmWorker != null ? farmWorker.getId() : null
        );
    }

    public TrainingAttendeeId getId() {
        return id;
    }

    public void setId(TrainingAttendeeId id) {
        this.id = id;
    }

    public TrainingRecord getTrainingRecord() {
        return trainingRecord;
    }

    public void setTrainingRecord(TrainingRecord trainingRecord) {
        this.trainingRecord = trainingRecord;
        if (trainingRecord != null) {
            if (this.id == null) this.id = new TrainingAttendeeId();
            this.id.setTrainingRecordId(trainingRecord.getId());
        }
    }

    public FarmWorker getFarmWorker() {
        return farmWorker;
    }

    public void setFarmWorker(FarmWorker farmWorker) {
        this.farmWorker = farmWorker;
        if (farmWorker != null) {
            if (this.id == null) this.id = new TrainingAttendeeId();
            this.id.setFarmWorkerId(farmWorker.getId());
        }
    }

    public String getAttendanceStatus() {
        return attendanceStatus;
    }

    public void setAttendanceStatus(String attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

