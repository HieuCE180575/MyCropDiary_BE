package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.HarvestRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository truy vấn dữ liệu bản ghi thu hoạch (HarvestRecord).
 */
@Repository
public interface HarvestRecordRepository extends JpaRepository<HarvestRecord, Long> {
    Page<HarvestRecord> findByCropSeasonId(Long cropSeasonId, Pageable pageable);
    boolean existsByHarvestLotCode(String harvestLotCode);
    boolean existsByTraceabilityCode(String traceabilityCode);
    Optional<HarvestRecord> findByIdAndCropSeasonId(Long id, Long cropSeasonId);
}
