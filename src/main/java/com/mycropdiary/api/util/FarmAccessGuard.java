package com.mycropdiary.api.util;

import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.Zone;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.BlockRepository;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.StaffZoneAssignmentRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.repository.ZoneRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Lớp hỗ trợ bảo mật và phân quyền truy cập theo phạm vi trang trại (Farm Scope & Staff Zone Scope).
 */
@Component
public class FarmAccessGuard {
    private final FarmMemberRepository farmMemberRepository;
    private final UserRepository userRepository;
    private final StaffZoneAssignmentRepository staffZoneAssignmentRepository;
    private final ZoneRepository zoneRepository;
    private final BlockRepository blockRepository;

    public FarmAccessGuard(FarmMemberRepository farmMemberRepository,
                           UserRepository userRepository,
                           StaffZoneAssignmentRepository staffZoneAssignmentRepository,
                           ZoneRepository zoneRepository,
                           BlockRepository blockRepository) {
        this.farmMemberRepository = farmMemberRepository;
        this.userRepository = userRepository;
        this.staffZoneAssignmentRepository = staffZoneAssignmentRepository;
        this.zoneRepository = zoneRepository;
        this.blockRepository = blockRepository;
    }

    /**
     * Kiểm tra người dùng có phải là Quản trị viên hệ thống (System ADMIN) hay không.
     */
    public boolean isSystemAdmin(UUID userId) {
        if (userId == null) {
            return false;
        }
        return userRepository.findById(userId)
                .map(user -> "admin".equalsIgnoreCase(user.getSystemRole()))
                .orElse(false);
    }

    /**
     * Lấy vai trò hoạt động của người dùng trong một trang trại cụ thể.
     */
    public String getUserRoleInFarm(UUID userId, UUID farmId) {
        if (isSystemAdmin(userId)) {
            return "admin";
        }
        return farmMemberRepository.findActiveRoleInFarm(farmId, userId)
                .orElse(null);
    }

    /**
     * Yêu cầu người dùng phải là thành viên đang hoạt động (active) của trang trại.
     */
    public FarmMember requireFarmMember(UUID userId, UUID farmId) {
        if (isSystemAdmin(userId)) {
            return null;
        }
        return farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, userId, "active")
                .orElseThrow(() -> new AccessDeniedException("Access denied: You are not an active member of this farm (Farm ID: " + farmId + ")"));
    }

    public FarmMember requireFarmAccess(UUID userId, UUID farmId) {
        if (isSystemAdmin(userId)) {
            return farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, userId, "active").orElse(null);
        }
        return farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, userId, "active")
                .orElseThrow(() -> new AccessDeniedException("Bạn không có quyền truy cập vào trang trại này (yêu cầu là thành viên active)"));
    }

    /**
     * Yêu cầu người dùng phải là Chủ trang trại (Farm OWNER) hoặc Quản trị hệ thống (System ADMIN).
     */
    public FarmMember requireOwnerOrAdmin(UUID userId, UUID farmId) {
        if (isSystemAdmin(userId)) {
            return null;
        }
        FarmMember member = requireFarmMember(userId, farmId);
        if (!member.isOwner()) {
            throw new AccessDeniedException("Access denied: Only Farm owner or System admin can perform this action");
        }
        return member;
    }

    public FarmMember requireFarmOwner(UUID userId, UUID farmId) {
        if (isSystemAdmin(userId)) {
            return farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, userId, "active").orElse(null);
        }
        FarmMember member = requireFarmAccess(userId, farmId);
        if (member == null || !member.isOwner()) {
            throw new AccessDeniedException("Thao tác yêu cầu quyền owner của trang trại");
        }
        return member;
    }

    public void requireZoneAccess(UUID userId, UUID farmId, UUID zoneId) {
        if (isSystemAdmin(userId)) {
            return;
        }
        FarmMember member = requireFarmMember(userId, farmId);
        if (member.isOwner()) {
            return;
        }
        boolean isAssigned = staffZoneAssignmentRepository.existsByFarmMemberIdAndZoneIdAndEndedAtIsNull(member.getId(), zoneId);
        if (!isAssigned) {
            throw new AccessDeniedException("Access denied: Staff is not assigned to Zone ID: " + zoneId);
        }
    }

    public Zone requireZoneAccess(UUID userId, UUID zoneId) {
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khu vực sản xuất ID: " + zoneId));

        if (isSystemAdmin(userId)) {
            return zone;
        }

        FarmMember member = requireFarmAccess(userId, zone.getFarm().getId());
        if (member != null && member.isOwner()) {
            return zone;
        }

        if (member == null) {
            throw new AccessDeniedException("Bạn không phải thành viên của trang trại này");
        }

        boolean isAssigned = staffZoneAssignmentRepository.existsByFarmMemberIdAndZoneIdAndEndedAtIsNull(member.getId(), zoneId);
        if (!isAssigned) {
            throw new AccessDeniedException("Nhân viên không được phân công phụ trách khu vực sản xuất này");
        }

        return zone;
    }

    public Block requireBlockAccess(UUID userId, UUID blockId) {
        Block block = blockRepository.findById(blockId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thửa đất ID: " + blockId));

        requireZoneAccess(userId, block.getZone().getId());
        return block;
    }

    public boolean isOwner(UUID userId, UUID farmId) {
        if (isSystemAdmin(userId)) {
            return true;
        }
        return farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, userId, "active")
                .map(FarmMember::isOwner)
                .orElse(false);
    }
}
