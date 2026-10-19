package com.mycropdiary.api.entity.cultivation;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class ActivityWorkerId implements Serializable {
    private UUID farmingActivity;
    private UUID farmWorker;

    public ActivityWorkerId() { }

    public ActivityWorkerId(UUID farmingActivity, UUID farmWorker) {
        this.farmingActivity = farmingActivity;
        this.farmWorker = farmWorker;
    }

    public UUID getFarmingActivity() { return farmingActivity; }
    public void setFarmingActivity(UUID farmingActivity) { this.farmingActivity = farmingActivity; }

    public UUID getFarmWorker() { return farmWorker; }
    public void setFarmWorker(UUID farmWorker) { this.farmWorker = farmWorker; }

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
