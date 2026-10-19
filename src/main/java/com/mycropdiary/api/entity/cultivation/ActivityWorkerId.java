package com.mycropdiary.api.entity.cultivation;

import java.io.Serializable;
import java.util.Objects;

public class ActivityWorkerId implements Serializable {
    private Integer farmingActivity;
    private Integer farmWorker;

    public ActivityWorkerId() { }

    public ActivityWorkerId(Integer farmingActivity, Integer farmWorker) {
        this.farmingActivity = farmingActivity;
        this.farmWorker = farmWorker;
    }

    public Integer getFarmingActivity() { return farmingActivity; }
    public void setFarmingActivity(Integer farmingActivity) { this.farmingActivity = farmingActivity; }

    public Integer getFarmWorker() { return farmWorker; }
    public void setFarmWorker(Integer farmWorker) { this.farmWorker = farmWorker; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActivityWorkerId that = (ActivityWorkerId) o;
        return Objects.equals(farmingActivity, that.farmingActivity) &&
                Objects.equals(farmWorker, that.farmWorker);
    }

    @Override
    public int hashCode() {
        return Objects.hash(farmingActivity, farmWorker);
    }
}
