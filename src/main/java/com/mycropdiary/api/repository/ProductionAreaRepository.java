package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.ProductionArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository thao tác cơ sở dữ liệu cho Entity ProductionArea (Vùng sản xuất).
 */
public interface ProductionAreaRepository extends JpaRepository<ProductionArea, Long> {
    /** Lấy danh sách vùng sản xuất theo Farm ID */
    List<ProductionArea> findByFarmId(Long farmId);

    /** Lấy danh sách vùng sản xuất theo Farm ID và trạng thái */
    List<ProductionArea> findByFarmIdAndStatus(Long farmId, String status);

    /** Tìm vùng sản xuất theo Mã vùng trong một trang trại */
    Optional<ProductionArea> findByFarmIdAndAreaCode(Long farmId, String areaCode);

    /** BR-05: Kiểm tra sự tồn tại của Mã vùng sản xuất trong trang trại */
    boolean existsByFarmIdAndAreaCode(Long farmId, String areaCode);

    /** BR-04: Tính tổng diện tích các Vùng sản xuất active trong trang trại (ngoại trừ một vùng nếu đang update) */
    @Query("SELECT COALESCE(SUM(pa.areaM2), 0) FROM ProductionArea pa WHERE pa.farm.id = :farmId AND pa.status = 'ACTIVE' AND (:excludeAreaId IS NULL OR pa.id <> :excludeAreaId)")
    BigDecimal sumActiveAreaM2ByFarmId(@Param("farmId") Long farmId, @Param("excludeAreaId") Long excludeAreaId);

    Optional<ProductionArea> findByIdAndFarmId(Long id, Long farmId);
    boolean existsByFarmIdAndAreaCodeIgnoreCase(Long farmId, String areaCode);
    boolean existsByFarmIdAndAreaCodeIgnoreCaseAndIdNot(Long farmId, String areaCode, Long id);

    @Query("SELECT pa FROM ProductionArea pa WHERE pa.farm.id = :farmId AND (:status IS NULL OR pa.status = :status) ORDER BY pa.areaName ASC")
    List<ProductionArea> searchAreas(@Param("farmId") Long farmId, @Param("status") String status);
}
