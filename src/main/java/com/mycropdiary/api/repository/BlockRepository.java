package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.Block;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlockRepository extends JpaRepository<Block, UUID> {
    List<Block> findByZoneId(UUID zoneId);
    List<Block> findByZoneFarmId(UUID farmId);
    Optional<Block> findByIdAndZoneId(UUID id, UUID zoneId);
    boolean existsByZoneIdAndBlockNameIgnoreCase(UUID zoneId, String blockName);
    boolean existsByZoneIdAndBlockNameIgnoreCaseAndIdNot(UUID zoneId, String blockName, UUID id);

    @Query("SELECT COALESCE(SUM(b.areaValue), 0) FROM Block b WHERE b.zone.id = :zoneId AND (:excludeBlockId IS NULL OR b.id != :excludeBlockId)")
    BigDecimal sumAreaValueByZoneId(@Param("zoneId") UUID zoneId, @Param("excludeBlockId") UUID excludeBlockId);

    @Query(value = "SELECT b FROM Block b " +
           "JOIN FETCH b.zone z " +
           "WHERE z.farm.id = :farmId " +
           "AND (:zoneId IS NULL OR z.id = :zoneId) " +
           "AND (:minArea IS NULL OR b.areaValue >= :minArea) " +
           "AND (:maxArea IS NULL OR b.areaValue <= :maxArea) " +
           "AND (:allowedZoneIds IS NULL OR z.id IN :allowedZoneIds) " +
           "AND (:keyword IS NULL OR LOWER(b.blockName) LIKE LOWER(CONCAT('%', :keyword, '%')))",
           countQuery = "SELECT count(b) FROM Block b " +
           "WHERE b.zone.farm.id = :farmId " +
           "AND (:zoneId IS NULL OR b.zone.id = :zoneId) " +
           "AND (:minArea IS NULL OR b.areaValue >= :minArea) " +
           "AND (:maxArea IS NULL OR b.areaValue <= :maxArea) " +
           "AND (:allowedZoneIds IS NULL OR b.zone.id IN :allowedZoneIds) " +
           "AND (:keyword IS NULL OR LOWER(b.blockName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Block> searchBlocksPaged(
            @Param("farmId") UUID farmId,
            @Param("zoneId") UUID zoneId,
            @Param("keyword") String keyword,
            @Param("minArea") BigDecimal minArea,
            @Param("maxArea") BigDecimal maxArea,
            @Param("allowedZoneIds") Collection<UUID> allowedZoneIds,
            Pageable pageable
    );

    @Query("SELECT b FROM Block b " +
           "JOIN FETCH b.zone z " +
           "WHERE z.farm.id = :farmId " +
           "AND (:zoneId IS NULL OR z.id = :zoneId) " +
           "AND (:allowedZoneIds IS NULL OR z.id IN :allowedZoneIds) " +
           "AND (:keyword IS NULL OR LOWER(b.blockName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Block> searchBlocks(
            @Param("farmId") UUID farmId,
            @Param("zoneId") UUID zoneId,
            @Param("keyword") String keyword,
            @Param("allowedZoneIds") Collection<UUID> allowedZoneIds
    );
}
