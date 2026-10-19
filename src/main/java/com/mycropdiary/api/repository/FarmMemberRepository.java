package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.FarmMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository thao tác cơ sở dữ liệu cho Entity FarmMember (Thành viên trang trại).
 */
public interface FarmMemberRepository extends JpaRepository<FarmMember, UUID> {
    Optional<FarmMember> findByFarmIdAndUserId(UUID farmId, UUID userId);

    @Query("SELECT fm FROM FarmMember fm WHERE fm.farm.id = :farmId AND fm.user.id = :userId AND LOWER(fm.membershipStatus) = LOWER(:status)")
    Optional<FarmMember> findByFarmIdAndUserIdAndStatus(@Param("farmId") UUID farmId, @Param("userId") UUID userId, @Param("status") String status);

    @Query("SELECT fm FROM FarmMember fm WHERE fm.farm.id = :farmId AND LOWER(fm.membershipStatus) = LOWER(:status)")
    List<FarmMember> findByFarmIdAndStatus(@Param("farmId") UUID farmId, @Param("status") String status);

    List<FarmMember> findByFarmId(UUID farmId);

    @Query("SELECT fm FROM FarmMember fm WHERE fm.user.id = :userId AND LOWER(fm.membershipStatus) = LOWER(:status)")
    List<FarmMember> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") String status);

    @Query("SELECT COUNT(fm) > 0 FROM FarmMember fm WHERE fm.farm.id = :farmId AND fm.user.id = :userId AND LOWER(fm.membershipStatus) = LOWER(:status)")
    boolean existsByFarmIdAndUserIdAndStatus(@Param("farmId") UUID farmId, @Param("userId") UUID userId, @Param("status") String status);

    @Query("SELECT COUNT(fm) > 0 FROM FarmMember fm WHERE fm.farm.id = :farmId AND LOWER(fm.memberRole) = LOWER(:farmRole) AND LOWER(fm.membershipStatus) = LOWER(:status)")
    boolean existsByFarmIdAndFarmRoleAndStatus(@Param("farmId") UUID farmId, @Param("farmRole") String farmRole, @Param("status") String status);

    @Query("SELECT COUNT(fm) FROM FarmMember fm WHERE fm.farm.id = :farmId AND LOWER(fm.memberRole) = LOWER(:farmRole) AND LOWER(fm.membershipStatus) = LOWER(:status)")
    long countByFarmIdAndFarmRoleAndStatus(@Param("farmId") UUID farmId, @Param("farmRole") String farmRole, @Param("status") String status);

    @Query("SELECT fm.memberRole FROM FarmMember fm WHERE fm.farm.id = :farmId AND fm.user.id = :userId AND LOWER(fm.membershipStatus) = 'active'")
    Optional<String> findActiveRoleInFarm(@Param("farmId") UUID farmId, @Param("userId") UUID userId);

    boolean existsByFarmIdAndUserId(UUID farmId, UUID userId);

    @Query("SELECT fm FROM FarmMember fm " +
           "JOIN FETCH fm.user u " +
           "WHERE fm.farm.id = :farmId " +
           "AND (:status IS NULL OR LOWER(fm.membershipStatus) = LOWER(:status)) " +
           "AND (:keyword IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY fm.joinedAt DESC")
    List<FarmMember> searchMembers(@Param("farmId") UUID farmId, 
                                   @Param("status") String status, 
                                   @Param("keyword") String keyword);
}
