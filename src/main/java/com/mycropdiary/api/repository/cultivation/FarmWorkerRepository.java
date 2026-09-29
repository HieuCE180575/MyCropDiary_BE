package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.FarmWorker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface FarmWorkerRepository extends JpaRepository<FarmWorker, Long> {
    List<FarmWorker> findByFarmId(Long farmId);
    List<FarmWorker> findByIdInAndFarmId(Collection<Long> ids, Long farmId);
}
