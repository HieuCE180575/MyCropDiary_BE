package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.Farm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository thao tác cơ sở dữ liệu cho Entity Farm (Trang trại).
 */
public interface FarmRepository extends JpaRepository<Farm, UUID> {

    @Query("SELECT DISTINCT f FROM Farm f JOIN FarmMember fm ON f.id = fm.farm.id " +
           "WHERE fm.user.id = :userId AND LOWER(fm.membershipStatus) = 'active' " +
           "AND (:keyword IS NULL OR LOWER(f.farmName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(f.address) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:status IS NULL OR LOWER(f.farmStatus) = LOWER(:status))")
    Page<Farm> findAccessibleFarmsByUser(@Param("userId") UUID userId,
                                         @Param("keyword") String keyword,
                                         @Param("status") String status,
                                         @Param("province") String province,
                                         Pageable pageable);

    @Query("SELECT DISTINCT f FROM Farm f JOIN FarmMember fm ON f.id = fm.farm.id " +
           "WHERE fm.user.id = :userId AND LOWER(fm.membershipStatus) = 'active'")
    List<Farm> findAllAccessibleFarmsByUser(@Param("userId") UUID userId);

    @Query("SELECT f FROM Farm f " +
           "JOIN FarmMember fm ON fm.farm.id = f.id " +
           "WHERE fm.user.id = :userId AND LOWER(fm.membershipStatus) = 'active' " +
           "AND (:status IS NULL OR LOWER(f.farmStatus) = LOWER(:status)) " +
           "AND (:keyword IS NULL OR LOWER(f.farmName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(f.address) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Farm> searchAccessibleFarms(
            @Param("userId") UUID userId,
            @Param("keyword") String keyword,
            @Param("province") String province,
            @Param("status") String status,
            @Param("minArea") BigDecimal minArea,
            @Param("maxArea") BigDecimal maxArea,
            Pageable pageable);
}
