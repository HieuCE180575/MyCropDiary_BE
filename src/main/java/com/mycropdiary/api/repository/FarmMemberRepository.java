package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.FarmMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FarmMemberRepository extends JpaRepository<FarmMember, Long> {
    Optional<FarmMember> findByFarmIdAndUserId(Long farmId, Long userId);
    Optional<FarmMember> findByFarmIdAndUserIdAndStatus(Long farmId, Long userId, String status);
    List<FarmMember> findByFarmIdAndStatus(Long farmId, String status);
    List<FarmMember> findByFarmId(Long farmId);
    List<FarmMember> findByUserIdAndStatus(Long userId, String status);

    boolean existsByFarmIdAndUserIdAndStatus(Long farmId, Long userId, String status);

    @Query("SELECT fm.farmRole FROM FarmMember fm WHERE fm.farm.id = :farmId AND fm.user.id = :userId AND fm.status = 'ACTIVE'")
    Optional<String> findActiveRoleInFarm(@Param("farmId") Long farmId, @Param("userId") Long userId);
}
