package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.CropSeason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CropSeasonRepository extends JpaRepository<CropSeason, Long> {
    List<CropSeason> findByFarmId(Long farmId);
    List<CropSeason> findByFarmIdAndStatus(Long farmId, String status);
}
