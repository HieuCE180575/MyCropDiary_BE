package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.FarmingActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FarmingActivityRepository extends JpaRepository<FarmingActivity, Long> {
    Page<FarmingActivity> findByCropSeasonId(Long cropSeasonId, Pageable pageable);
    Optional<FarmingActivity> findByIdAndCropSeasonId(Long id, Long cropSeasonId);
}
