package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.MaterialUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository truy vấn nhật ký sử dụng vật tư (MaterialUsage).
 */
@Repository
public interface MaterialUsageRepository extends JpaRepository<MaterialUsage, UUID> {
    List<MaterialUsage> findByFarmingActivityId(UUID farmingActivityId);
    Page<MaterialUsage> findByFarmingActivityId(UUID farmingActivityId, Pageable pageable);
    Optional<MaterialUsage> findByIdAndFarmingActivityId(UUID id, UUID farmingActivityId);
}
