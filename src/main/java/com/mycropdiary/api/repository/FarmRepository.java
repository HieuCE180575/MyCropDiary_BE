package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.Farm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository thao tác cơ sở dữ liệu cho Entity Farm (Trang trại).
 */
public interface FarmRepository extends JpaRepository<Farm, Long> {
    /** Tìm trang trại theo mã trang trại duy nhất */
    Optional<Farm> findByFarmCode(String farmCode);

    /** Kiểm tra sự tồn tại của mã trang trại */
    boolean existsByFarmCode(String farmCode);

    /**
     * Tìm kiếm và phân trang danh sách các trang trại người dùng có quyền truy cập.
     */
    @Query("SELECT DISTINCT f FROM Farm f JOIN FarmMember fm ON f.id = fm.farm.id " +
           "WHERE fm.user.id = :userId AND fm.status = 'ACTIVE' " +
           "AND (:keyword IS NULL OR LOWER(f.farmName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(f.farmCode) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:status IS NULL OR f.status = :status) " +
           "AND (:province IS NULL OR f.province = :province)")
    Page<Farm> findAccessibleFarmsByUser(@Param("userId") Long userId,
                                         @Param("keyword") String keyword,
                                         @Param("status") String status,
                                         @Param("province") String province,
                                         Pageable pageable);

    /**
     * Lấy tất cả trang trại mà người dùng có quyền truy cập.
     */
    @Query("SELECT DISTINCT f FROM Farm f JOIN FarmMember fm ON f.id = fm.farm.id " +
           "WHERE fm.user.id = :userId AND fm.status = 'ACTIVE'")
    List<Farm> findAllAccessibleFarmsByUser(@Param("userId") Long userId);

    @Query("SELECT f FROM Farm f " +
           "JOIN FarmMember fm ON fm.farm.id = f.id " +
           "WHERE fm.user.id = :userId AND fm.status = 'ACTIVE' " +
           "AND (:status IS NULL OR f.status = :status) " +
           "AND (:province IS NULL OR LOWER(f.province) LIKE LOWER(CONCAT('%', :province, '%'))) " +
           "AND (:minArea IS NULL OR f.totalAreaM2 >= :minArea) " +
           "AND (:maxArea IS NULL OR f.totalAreaM2 <= :maxArea) " +
           "AND (:keyword IS NULL OR LOWER(f.farmName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(f.farmCode) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(f.addressLine) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Farm> searchAccessibleFarms(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("province") String province,
            @Param("status") String status,
            @Param("minArea") java.math.BigDecimal minArea,
            @Param("maxArea") java.math.BigDecimal maxArea,
            Pageable pageable
    );

    default List<Farm> findAllAccessibleByUserId(Long userId) {
        return findAllAccessibleFarmsByUser(userId);
    }
}
