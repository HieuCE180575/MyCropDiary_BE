package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.Plot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlotRepository extends JpaRepository<Plot, Long> {
    List<Plot> findByProductionAreaId(Long productionAreaId);
    List<Plot> findByProductionAreaFarmId(Long farmId);
    Optional<Plot> findByIdAndProductionAreaId(Long id, Long productionAreaId);
    boolean existsByProductionAreaIdAndPlotCodeIgnoreCase(Long productionAreaId, String plotCode);
    boolean existsByProductionAreaIdAndPlotCodeIgnoreCaseAndIdNot(Long productionAreaId, String plotCode, Long id);

    @Query("SELECT COALESCE(SUM(p.areaM2), 0) FROM Plot p WHERE p.productionArea.id = :areaId AND p.status != 'INACTIVE' AND (:excludePlotId IS NULL OR p.id != :excludePlotId)")
    BigDecimal sumActiveAreaM2ByProductionAreaId(@Param("areaId") Long areaId, @Param("excludePlotId") Long excludePlotId);

    @Query(value = "SELECT p FROM Plot p " +
           "JOIN FETCH p.productionArea pa " +
           "WHERE pa.farm.id = :farmId " +
           "AND (:productionAreaId IS NULL OR pa.id = :productionAreaId) " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:minArea IS NULL OR p.areaM2 >= :minArea) " +
           "AND (:maxArea IS NULL OR p.areaM2 <= :maxArea) " +
           "AND (:allowedAreaIds IS NULL OR pa.id IN :allowedAreaIds) " +
           "AND (:keyword IS NULL OR LOWER(p.plotCode) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(p.plotName) LIKE LOWER(CONCAT('%', :keyword, '%')))",
           countQuery = "SELECT count(p) FROM Plot p " +
           "WHERE p.productionArea.farm.id = :farmId " +
           "AND (:productionAreaId IS NULL OR p.productionArea.id = :productionAreaId) " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:minArea IS NULL OR p.areaM2 >= :minArea) " +
           "AND (:maxArea IS NULL OR p.areaM2 <= :maxArea) " +
           "AND (:allowedAreaIds IS NULL OR p.productionArea.id IN :allowedAreaIds) " +
           "AND (:keyword IS NULL OR LOWER(p.plotCode) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(p.plotName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Plot> searchPlotsPaged(
            @Param("farmId") Long farmId,
            @Param("productionAreaId") Long productionAreaId,
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("minArea") BigDecimal minArea,
            @Param("maxArea") BigDecimal maxArea,
            @Param("allowedAreaIds") Collection<Long> allowedAreaIds,
            Pageable pageable
    );

    @Query("SELECT p FROM Plot p " +
           "JOIN FETCH p.productionArea pa " +
           "WHERE pa.farm.id = :farmId " +
           "AND (:productionAreaId IS NULL OR pa.id = :productionAreaId) " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:keyword IS NULL OR LOWER(p.plotCode) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(p.plotName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY p.plotCode ASC")
    List<Plot> searchPlots(@Param("farmId") Long farmId,
                          @Param("productionAreaId") Long productionAreaId,
                          @Param("status") String status,
                          @Param("keyword") String keyword);
}
