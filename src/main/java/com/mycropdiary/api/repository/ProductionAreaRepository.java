package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.ProductionArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductionAreaRepository extends JpaRepository<ProductionArea, Long> {
    List<ProductionArea> findByFarmId(Long farmId);
    List<ProductionArea> findByFarmIdAndStatus(Long farmId, String status);
    Optional<ProductionArea> findByFarmIdAndAreaCode(Long farmId, String areaCode);
    boolean existsByFarmIdAndAreaCode(Long farmId, String areaCode);

    @Query("SELECT COALESCE(SUM(pa.areaM2), 0) FROM ProductionArea pa WHERE pa.farm.id = :farmId AND pa.status = 'ACTIVE' AND (:excludeAreaId IS NULL OR pa.id <> :excludeAreaId)")
    BigDecimal sumActiveAreaM2ByFarmId(@Param("farmId") Long farmId, @Param("excludeAreaId") Long excludeAreaId);
}
