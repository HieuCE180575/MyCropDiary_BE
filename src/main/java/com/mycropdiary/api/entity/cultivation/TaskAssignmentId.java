package com.mycropdiary.api.entity.cultivation;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TaskAssignmentId implements Serializable {

    @Column(name = "farm_task_id")
    private Integer farmTaskId;

    @Column(name = "farm_worker_id")
    private Integer farmWorkerId;

    public TaskAssignmentId() {}

    public TaskAssignmentId(Integer farmTaskId, Integer farmWorkerId) {
        this.farmTaskId = farmTaskId;
        this.farmWorkerId = farmWorkerId;
    }

    public Integer getFarmTaskId() { return farmTaskId; }
    public void setFarmTaskId(Integer farmTaskId) { this.farmTaskId = farmTaskId; }

    public Integer getFarmWorkerId() { return farmWorkerId; }
    public void setFarmWorkerId(Integer farmWorkerId) { this.farmWorkerId = farmWorkerId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaskAssignmentId that)) return false;
        return Objects.equals(farmTaskId, that.farmTaskId) && Objects.equals(farmWorkerId, that.farmWorkerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(farmTaskId, farmWorkerId);
    }
}
