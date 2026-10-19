package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository thao tác cơ sở dữ liệu cho Entity Zone (Khu vực / Phân khu sản xuất).
 */
public interface ZoneRepository extends JpaRepository<Zone, UUID> {
    List<Zone> findByFarmId(UUID farmId);

    Optional<Zone> findByFarmIdAndZoneName(UUID farmId, String zoneName);

    boolean existsByFarmIdAndZoneNameIgnoreCase(UUID farmId, String zoneName);

    boolean existsByFarmIdAndZoneNameIgnoreCaseAndIdNot(UUID farmId, String zoneName, UUID id);

    Optional<Zone> findByIdAndFarmId(UUID id, UUID farmId);

    @Query("SELECT COALESCE(SUM(z.areaValue), 0) FROM Zone z WHERE z.farm.id = :farmId AND (:excludeZoneId IS NULL OR z.id <> :excludeZoneId)")
    BigDecimal sumAreaValueByFarmId(@Param("farmId") UUID farmId, @Param("excludeZoneId") UUID excludeZoneId);

    @Query("SELECT z FROM Zone z WHERE z.farm.id = :farmId " +
           "AND (:keyword IS NULL OR LOWER(z.zoneName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY z.zoneName ASC")
    List<Zone> searchZones(@Param("farmId") UUID farmId, @Param("keyword") String keyword);
}
