package com.mycropdiary.api.entity.cultivation;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite Primary Key cho Entity ActivityWorker.
 */
public class ActivityWorkerId implements Serializable {

    private Long farmingActivity;
    private Long farmWorker;

    public ActivityWorkerId() { }

    public ActivityWorkerId(Long farmingActivity, Long farmWorker) {
        this.farmingActivity = farmingActivity;
        this.farmWorker = farmWorker;
    }

    public Long getFarmingActivity() { return farmingActivity; }
    public void setFarmingActivity(Long farmingActivity) { this.farmingActivity = farmingActivity; }

    public Long getFarmWorker() { return farmWorker; }
    public void setFarmWorker(Long farmWorker) { this.farmWorker = farmWorker; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActivityWorkerId that = (ActivityWorkerId) o;
        return Objects.equals(farmingActivity, that.farmingActivity) && Objects.equals(farmWorker, that.farmWorker);
    }

    @Override
    public int hashCode() {
        return Objects.hash(farmingActivity, farmWorker);
    }
}
