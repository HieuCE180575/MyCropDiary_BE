package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.StaffAreaAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

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
}
