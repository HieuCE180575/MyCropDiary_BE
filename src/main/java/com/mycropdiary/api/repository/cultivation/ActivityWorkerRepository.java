package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.ActivityWorker;
import com.mycropdiary.api.entity.cultivation.ActivityWorkerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityWorkerRepository extends JpaRepository<ActivityWorker, ActivityWorkerId> {
    List<ActivityWorker> findByFarmingActivityId(Long farmingActivityId);
}
