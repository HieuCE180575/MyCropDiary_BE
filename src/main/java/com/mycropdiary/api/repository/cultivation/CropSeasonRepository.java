package com.mycropdiary.api.repository.cultivation;

import com.mycropdiary.api.entity.cultivation.CropSeason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CropSeasonRepository extends JpaRepository<CropSeason, UUID> {
    List<CropSeason> findByFarmId(UUID farmId);

    @Query("SELECT cs FROM CropSeason cs WHERE cs.farm.id = :farmId AND LOWER(cs.seasonStatus) = LOWER(:status)")
    List<CropSeason> findByFarmIdAndStatus(@Param("farmId") UUID farmId, @Param("status") String status);
}
