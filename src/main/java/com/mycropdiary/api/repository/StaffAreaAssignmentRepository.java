package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.StaffAreaAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffAreaAssignmentRepository extends JpaRepository<StaffAreaAssignment, Long> {
    List<StaffAreaAssignment> findByFarmMemberIdAndActiveTrue(Long farmMemberId);
    List<StaffAreaAssignment> findByProductionAreaIdAndActiveTrue(Long productionAreaId);
    boolean existsByFarmMemberIdAndProductionAreaIdAndActiveTrue(Long farmMemberId, Long productionAreaId);
}
