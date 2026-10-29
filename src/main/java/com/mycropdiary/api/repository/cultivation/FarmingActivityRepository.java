package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.FarmingActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FarmingActivityRepository extends JpaRepository<FarmingActivity, Integer>, JpaSpecificationExecutor<FarmingActivity> {
    Page<FarmingActivity> findByCropSeasonId(Integer cropSeasonId, Pageable pageable);
    Optional<FarmingActivity> findByIdAndCropSeasonId(Integer id, Integer cropSeasonId);
    boolean existsByCropSeasonId(Integer cropSeasonId);
}
