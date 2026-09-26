package com.mycropdiary.api.util;

import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.repository.AppUserRepository;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.StaffAreaAssignmentRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class FarmAccessGuard {
    private final FarmMemberRepository farmMemberRepository;
    private final AppUserRepository appUserRepository;
    private final StaffAreaAssignmentRepository staffAreaAssignmentRepository;

    public FarmAccessGuard(FarmMemberRepository farmMemberRepository,
                           AppUserRepository appUserRepository,
                           StaffAreaAssignmentRepository staffAreaAssignmentRepository) {
        this.farmMemberRepository = farmMemberRepository;
        this.appUserRepository = appUserRepository;
        this.staffAreaAssignmentRepository = staffAreaAssignmentRepository;
    }

    public boolean isSystemAdmin(Long userId) {
        return appUserRepository.findById(userId)
                .map(user -> "ADMIN".equalsIgnoreCase(user.getSystemRole()))
                .orElse(false);
    }

    public String getUserRoleInFarm(Long userId, Long farmId) {
        if (isSystemAdmin(userId)) {
            return "ADMIN";
        }
        return farmMemberRepository.findActiveRoleInFarm(farmId, userId)
                .orElse(null);
    }

    public FarmMember requireFarmMember(Long userId, Long farmId) {
        if (isSystemAdmin(userId)) {
            return null; // System admin bypasses member requirement
        }
        return farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, userId, "ACTIVE")
                .orElseThrow(() -> new AccessDeniedException("Access denied: You are not an active member of this farm (Farm ID: " + farmId + ")"));
    }

    public FarmMember requireOwnerOrAdmin(Long userId, Long farmId) {
        if (isSystemAdmin(userId)) {
            return null;
        }
        FarmMember member = requireFarmMember(userId, farmId);
        if (!"OWNER".equalsIgnoreCase(member.getFarmRole())) {
            throw new AccessDeniedException("Access denied: Only Farm OWNER or System ADMIN can perform this action");
        }
        return member;
    }

    public void requireProductionAreaAccess(Long userId, Long farmId, Long productionAreaId) {
        if (isSystemAdmin(userId)) {
            return;
        }
        FarmMember member = requireFarmMember(userId, farmId);
        if ("OWNER".equalsIgnoreCase(member.getFarmRole())) {
            return; // OWNER has access to all production areas in the farm
        }
        boolean isAssigned = staffAreaAssignmentRepository.existsByFarmMemberIdAndProductionAreaIdAndActiveTrue(member.getId(), productionAreaId);
        if (!isAssigned) {
            throw new AccessDeniedException("Access denied: Staff is not assigned to Production Area ID: " + productionAreaId);
        }
    }
}
