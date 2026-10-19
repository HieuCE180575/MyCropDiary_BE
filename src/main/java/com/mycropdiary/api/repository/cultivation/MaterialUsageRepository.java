package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.MaterialUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy vấn nhật ký sử dụng vật tư (MaterialUsage).
 */
@Repository
public interface MaterialUsageRepository extends JpaRepository<MaterialUsage, Integer> {
    List<MaterialUsage> findByFarmingActivityId(Integer farmingActivityId);
    Page<MaterialUsage> findByFarmingActivityId(Integer farmingActivityId, Pageable pageable);
    Optional<MaterialUsage> findByIdAndFarmingActivityId(Integer id, Integer farmingActivityId);
}
