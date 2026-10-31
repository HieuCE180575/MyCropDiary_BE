package com.mycropdiary.api.entity.cultivation;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TrainingAttendeeId implements Serializable {

    @Column(name = "training_record_id")
    private Integer trainingRecordId;

    @Column(name = "farm_worker_id")
    private Integer farmWorkerId;

    public TrainingAttendeeId() {
    }

    public TrainingAttendeeId(Integer trainingRecordId, Integer farmWorkerId) {
        this.trainingRecordId = trainingRecordId;
        this.farmWorkerId = farmWorkerId;
    }

    public Integer getTrainingRecordId() {
        return trainingRecordId;
    }

    public void setTrainingRecordId(Integer trainingRecordId) {
        this.trainingRecordId = trainingRecordId;
    }

    public Integer getFarmWorkerId() {
        return farmWorkerId;
    }

    public void setFarmWorkerId(Integer farmWorkerId) {
        this.farmWorkerId = farmWorkerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrainingAttendeeId that = (TrainingAttendeeId) o;
        return Objects.equals(trainingRecordId, that.trainingRecordId) &&
                Objects.equals(farmWorkerId, that.farmWorkerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trainingRecordId, farmWorkerId);
    }
}

