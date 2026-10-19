package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository thao tác cơ sở dữ liệu cho Entity Zone (Khu vực / Phân khu sản xuất).
 */
public interface ZoneRepository extends JpaRepository<Zone, Integer> {
    List<Zone> findByFarmId(Integer farmId);

    Optional<Zone> findByFarmIdAndZoneName(Integer farmId, String zoneName);

    boolean existsByFarmIdAndZoneNameIgnoreCase(Integer farmId, String zoneName);

    boolean existsByFarmIdAndZoneNameIgnoreCaseAndIdNot(Integer farmId, String zoneName, Integer id);

    Optional<Zone> findByIdAndFarmId(Integer id, Integer farmId);

    @Query("SELECT COALESCE(SUM(z.areaValue), 0) FROM Zone z WHERE z.farm.id = :farmId AND (:excludeZoneId IS NULL OR z.id <> :excludeZoneId)")
    BigDecimal sumAreaValueByFarmId(@Param("farmId") Integer farmId, @Param("excludeZoneId") Integer excludeZoneId);

    @Query("SELECT z FROM Zone z WHERE z.farm.id = :farmId " +
           "AND (:keyword IS NULL OR LOWER(z.zoneName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY z.zoneName ASC")
    List<Zone> searchZones(@Param("farmId") Integer farmId, @Param("keyword") String keyword);
}
