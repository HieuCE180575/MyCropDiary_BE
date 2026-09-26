package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.FarmMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository thao tác cơ sở dữ liệu cho Entity FarmMember (Thành viên trang trại).
 */
public interface FarmMemberRepository extends JpaRepository<FarmMember, Long> {
    /** Tìm thành viên theo Farm ID và User ID */
    Optional<FarmMember> findByFarmIdAndUserId(Long farmId, Long userId);

    /** Tìm thành viên theo Farm ID, User ID và trạng thái */
    Optional<FarmMember> findByFarmIdAndUserIdAndStatus(Long farmId, Long userId, String status);

    /** Lấy danh sách thành viên trang trại theo trạng thái */
    List<FarmMember> findByFarmIdAndStatus(Long farmId, String status);

    /** Lấy toàn bộ danh sách thành viên của một trang trại */
    List<FarmMember> findByFarmId(Long farmId);

    /** Lấy danh sách thành viên theo User ID và trạng thái */
    List<FarmMember> findByUserIdAndStatus(Long userId, String status);

    /** Kiểm tra xem thành viên có tồn tại trong trang trại với trạng thái nhất định hay không */
    boolean existsByFarmIdAndUserIdAndStatus(Long farmId, Long userId, String status);

    /** BR-01: Kiểm tra trang trại đã có thành viên với vai trò và trạng thái nhất định (dùng kiểm tra Active OWNER) */
    boolean existsByFarmIdAndFarmRoleAndStatus(Long farmId, String farmRole, String status);

    /** BR-02: Đếm số lượng thành viên active theo vai trò (dùng bảo vệ Active OWNER duy nhất) */
    long countByFarmIdAndFarmRoleAndStatus(Long farmId, String farmRole, String status);

    /** Lấy vai trò active của người dùng trong trang trại */
    @Query("SELECT fm.farmRole FROM FarmMember fm WHERE fm.farm.id = :farmId AND fm.user.id = :userId AND fm.status = 'ACTIVE'")
    Optional<String> findActiveRoleInFarm(@Param("farmId") Long farmId, @Param("userId") Long userId);
}
