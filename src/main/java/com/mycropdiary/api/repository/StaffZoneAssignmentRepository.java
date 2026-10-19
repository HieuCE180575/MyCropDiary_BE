package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.StaffZoneAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository thao tác cơ sở dữ liệu cho Entity StaffZoneAssignment (Phân công nhân viên quản lý Zone).
 */
public interface StaffZoneAssignmentRepository extends JpaRepository<StaffZoneAssignment, Integer> {
    List<StaffZoneAssignment> findByFarmMemberIdAndEndedAtIsNull(Integer farmMemberId);

    List<StaffZoneAssignment> findByZoneIdAndEndedAtIsNull(Integer zoneId);

    boolean existsByFarmMemberIdAndZoneIdAndEndedAtIsNull(Integer farmMemberId, Integer zoneId);

    Optional<StaffZoneAssignment> findByFarmMemberIdAndZoneIdAndEndedAtIsNull(Integer farmMemberId, Integer zoneId);

    @Query("SELECT sa FROM StaffZoneAssignment sa " +
           "JOIN FETCH sa.zone z " +
           "WHERE sa.farmMember.id = :farmMemberId AND sa.endedAt IS NULL")
    List<StaffZoneAssignment> findActiveAssignmentsByMemberId(@Param("farmMemberId") Integer farmMemberId);

    @Query("SELECT sa FROM StaffZoneAssignment sa " +
           "JOIN FETCH sa.farmMember fm " +
           "JOIN FETCH fm.user u " +
           "WHERE sa.zone.id = :zoneId AND sa.endedAt IS NULL")
    List<StaffZoneAssignment> findActiveAssignmentsByZoneId(@Param("zoneId") Integer zoneId);

    @Query("SELECT sa FROM StaffZoneAssignment sa " +
           "JOIN FETCH sa.zone z " +
           "JOIN FETCH sa.farmMember fm " +
           "JOIN FETCH fm.user u " +
           "WHERE z.farm.id = :farmId " +
           "AND (:zoneId IS NULL OR z.id = :zoneId) " +
           "AND (:isActive IS NULL OR (:isActive = true AND sa.endedAt IS NULL) OR (:isActive = false AND sa.endedAt IS NOT NULL)) " +
           "ORDER BY sa.assignedAt DESC")
    List<StaffZoneAssignment> findAssignments(@Param("farmId") Integer farmId,
                                              @Param("zoneId") Integer zoneId,
                                              @Param("isActive") Boolean isActive);
}
