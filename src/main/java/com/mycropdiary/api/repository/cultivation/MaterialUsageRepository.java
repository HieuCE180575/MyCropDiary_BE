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
public interface MaterialUsageRepository extends JpaRepository<MaterialUsage, Long> {
    List<MaterialUsage> findByFarmingActivityId(Long farmingActivityId);
    Page<MaterialUsage> findByFarmingActivityId(Long farmingActivityId, Pageable pageable);
    Optional<MaterialUsage> findByIdAndFarmingActivityId(Long id, Long farmingActivityId);
}
