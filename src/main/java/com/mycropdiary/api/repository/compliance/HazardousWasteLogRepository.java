package com.mycropdiary.api.repository.compliance;

import com.mycropdiary.api.entity.compliance.HazardousWasteLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HazardousWasteLogRepository extends JpaRepository<HazardousWasteLog, Integer> {
    List<HazardousWasteLog> findByFarmIdOrderByCollectionDateDescIdDesc(Integer farmId);
}
