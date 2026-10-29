package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.HarvestRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository truy vấn dữ liệu bản ghi thu hoạch (HarvestRecord).
 */
@Repository
public interface HarvestRecordRepository extends JpaRepository<HarvestRecord, Integer>, JpaSpecificationExecutor<HarvestRecord> {
    Page<HarvestRecord> findByCropSeasonId(Integer cropSeasonId, Pageable pageable);
    Optional<HarvestRecord> findByIdAndCropSeasonId(Integer id, Integer cropSeasonId);
    boolean existsByCropSeasonId(Integer cropSeasonId);
}
