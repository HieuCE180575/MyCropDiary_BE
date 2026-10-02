package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.StaffAreaAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository thao tác cơ sở dữ liệu cho Entity StaffAreaAssignment (Phân công nhân viên quản lý vùng).
 */
public interface StaffAreaAssignmentRepository extends JpaRepository<StaffAreaAssignment, Long> {
    /** Lấy danh sách phân công đang hoạt động của thành viên trang trại */
    List<StaffAreaAssignment> findByFarmMemberIdAndActiveTrue(Long farmMemberId);

    /** Lấy danh sách phân công đang hoạt động cho một Vùng sản xuất */
    List<StaffAreaAssignment> findByProductionAreaIdAndActiveTrue(Long productionAreaId);

    /** BR-07: Kiểm tra xem thành viên đã được phân công active vào Vùng sản xuất hay chưa */
    boolean existsByFarmMemberIdAndProductionAreaIdAndActiveTrue(Long farmMemberId, Long productionAreaId);

    Optional<StaffAreaAssignment> findByFarmMemberIdAndProductionAreaIdAndActiveTrue(Long farmMemberId, Long productionAreaId);

    default List<StaffAreaAssignment> findByFarmMemberIdAndIsActiveTrue(Long farmMemberId) {
        return findByFarmMemberIdAndActiveTrue(farmMemberId);
    }

    default List<StaffAreaAssignment> findByProductionAreaIdAndIsActiveTrue(Long productionAreaId) {
        return findByProductionAreaIdAndActiveTrue(productionAreaId);
    }

    default Optional<StaffAreaAssignment> findByFarmMemberIdAndProductionAreaIdAndIsActiveTrue(Long farmMemberId, Long productionAreaId) {
        return findByFarmMemberIdAndProductionAreaIdAndActiveTrue(farmMemberId, productionAreaId);
    }

    default boolean existsByFarmMemberIdAndProductionAreaIdAndIsActiveTrue(Long farmMemberId, Long productionAreaId) {
        return existsByFarmMemberIdAndProductionAreaIdAndActiveTrue(farmMemberId, productionAreaId);
    }

    @Query("SELECT sa FROM StaffAreaAssignment sa " +
           "JOIN FETCH sa.productionArea pa " +
           "WHERE sa.farmMember.id = :farmMemberId AND sa.active = true")
    List<StaffAreaAssignment> findActiveAssignmentsByMemberId(@Param("farmMemberId") Long farmMemberId);

    @Query("SELECT sa FROM StaffAreaAssignment sa " +
           "JOIN FETCH sa.farmMember fm " +
           "JOIN FETCH fm.user u " +
           "WHERE sa.productionArea.id = :productionAreaId AND sa.active = true")
    List<StaffAreaAssignment> findActiveAssignmentsByAreaId(@Param("productionAreaId") Long productionAreaId);

    @Query("SELECT sa FROM StaffAreaAssignment sa " +
           "JOIN FETCH sa.productionArea pa " +
           "JOIN FETCH sa.farmMember fm " +
           "JOIN FETCH fm.user u " +
           "WHERE pa.farm.id = :farmId " +
           "AND (:productionAreaId IS NULL OR pa.id = :productionAreaId) " +
           "AND (:isActive IS NULL OR sa.active = :isActive) " +
           "ORDER BY sa.startDate DESC")
    List<StaffAreaAssignment> searchAssignments(@Param("farmId") Long farmId,
                                               @Param("productionAreaId") Long productionAreaId,
                                               @Param("isActive") Boolean isActive);
}
