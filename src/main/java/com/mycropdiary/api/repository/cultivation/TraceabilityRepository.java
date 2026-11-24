package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.HarvestRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TraceabilityRepository extends JpaRepository<HarvestRecord, Integer> {
    List<HarvestRecord> findByFarmId(Integer farmId);
}
