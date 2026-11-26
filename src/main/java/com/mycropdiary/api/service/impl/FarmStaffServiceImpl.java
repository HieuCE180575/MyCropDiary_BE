package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.staff.CandidateUserRespDTO;
import com.mycropdiary.api.dto.staff.FarmInvitationRespDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import java.util.Set;
import com.mycropdiary.api.dto.staff.CreateStaffReqDTO;
import com.mycropdiary.api.dto.staff.FarmMemberRespDTO;
import com.mycropdiary.api.dto.staff.UpdateStaffReqDTO;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.StaffZoneAssignment;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.exception.BusinessRuleException;
import com.mycropdiary.api.exception.ResourceConflictException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.StaffZoneAssignmentRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.mapper.FarmStaffMapper;
import com.mycropdiary.api.service.FarmStaffService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class FarmStaffServiceImpl implements FarmStaffService {
    private final FarmMemberRepository farmMemberRepository;
    private final FarmRepository farmRepository;
    private final UserRepository userRepository;
    private final StaffZoneAssignmentRepository assignmentRepository;
    private final FarmAccessGuard accessGuard;

    public FarmStaffServiceImpl(FarmMemberRepository farmMemberRepository,
                                FarmRepository farmRepository,
                                UserRepository userRepository,
                                StaffZoneAssignmentRepository assignmentRepository,
                                FarmAccessGuard accessGuard) {
        this.farmMemberRepository = farmMemberRepository;
        this.farmRepository = farmRepository;
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.accessGuard = accessGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FarmMemberRespDTO> getStaffList(Integer currentUserId, Integer farmId, String status, String keyword) {
        accessGuard.requireFarmAccess(currentUserId, farmId);

        String trimmedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String trimmedStatus = (status != null && !status.isBlank()) ? status.trim().toLowerCase() : null;

        List<FarmMember> members = farmMemberRepository.searchMembers(farmId, trimmedStatus, trimmedKeyword);
        if (members.isEmpty()) {
            return List.of();
        }

        Map<Integer, Integer> assignmentCountMap = assignmentRepository.countActiveAssignmentsGroupedByMember(farmId)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Integer) row[0],
                        row -> ((Number) row[1]).intValue(),
                        (existing, replacement) -> existing
                ));

        return members.stream()
                .map(member -> toResponse(member, assignmentCountMap.getOrDefault(member.getId(), 0)))
                .toList();
    }

    @Override
    public FarmMemberRespDTO addStaff(Integer currentUserId, Integer farmId, CreateStaffReqDTO request) {
        accessGuard.requireFarmOwner(currentUserId, farmId);

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trang trại ID: " + farmId));

        User user;
        if (request.email() != null && !request.email().isBlank()) {
            user = userRepository.findByEmailIgnoreCase(request.email().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với email: " + request.email()));
        } else if (request.userId() != null) {
            user = userRepository.findById(request.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với ID: " + request.userId()));
        } else {
            throw new BusinessRuleException("Vui lòng cung cấp email hoặc userId của nhân viên cần thêm");
        }

        if (!user.isActive()) {
            throw new BusinessRuleException("Tài khoản người dùng chưa được kích hoạt hoặc đang bị khóa (trạng thái: " + user.getAccountStatus() + ")");
        }

        String role = FarmMember.normalizeMemberRole(request.farmRole());

        Optional<FarmMember> existingMemberOpt = farmMemberRepository.findByFarmIdAndUserId(farmId, user.getId());
        FarmMember member;
        if (existingMemberOpt.isPresent()) {
            member = existingMemberOpt.get();
            if (member.isActive()) {
                throw new ResourceConflictException("Người dùng (" + user.getEmail() + ") đã là thành viên active trong trang trại này");
            }
            member.setMembershipStatus("active");
            member.setMemberRole(role);
            member.setJoinedAt(Instant.now());
        } else {
            member = new FarmMember(farm, user, role, "active");
        }
        if (request.jobTitle() != null && !request.jobTitle().isBlank()) {
            member.setJobTitle(request.jobTitle().trim());
        }

        // Sync User.system_role if currently basic 'user' (or upgrading 'staff' to 'owner')
        String currentSysRole = user.getSystemRole() != null ? user.getSystemRole().toLowerCase() : "user";
        if ("owner".equals(role) && ("user".equals(currentSysRole) || "staff".equals(currentSysRole))) {
            user.setSystemRole("owner");
            userRepository.save(user);
        } else if (!"owner".equals(role) && "user".equals(currentSysRole)) {
            user.setSystemRole("staff");
            userRepository.save(user);
        }

        FarmMember saved = farmMemberRepository.save(member);
        return toResponse(saved, 0);
    }

    @Override
    public FarmMemberRespDTO updateStaff(Integer currentUserId, Integer farmId, Integer memberId, UpdateStaffReqDTO request) {
        accessGuard.requireFarmOwner(currentUserId, farmId);

        FarmMember member = farmMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên ID: " + memberId));

        if (!member.getFarm().getId().equals(farmId)) {
            throw new BusinessRuleException("Thành viên không thuộc trang trại này");
        }

        if (request.farmRole() != null && !"owner".equalsIgnoreCase(request.farmRole()) && member.isOwner()) {
            long activeOwners = farmMemberRepository.countByFarmIdAndFarmRoleAndStatus(farmId, "owner", "active");
            if (activeOwners <= 1) {
                throw new BusinessRuleException("Không thể hạ quyền Chủ trang trại duy nhất còn lại của trang trại");
            }
            member.setMemberRole(FarmMember.normalizeMemberRole(request.farmRole()));
        } else if (request.farmRole() != null) {
            member.setMemberRole(FarmMember.normalizeMemberRole(request.farmRole()));
        }

        if (request.status() != null && !request.status().equalsIgnoreCase(member.getMembershipStatus())) {
            if ("suspended".equalsIgnoreCase(request.status()) || "left".equalsIgnoreCase(request.status()) || "inactive".equalsIgnoreCase(request.status())) {
                deactivateStaff(currentUserId, farmId, memberId);
                return toResponse(member, 0);
            }
            member.setMembershipStatus(request.status().toLowerCase());
        }

        FarmMember saved = farmMemberRepository.save(member);
        return toResponse(saved, null);
    }

    @Override
    public void deactivateStaff(Integer currentUserId, Integer farmId, Integer memberId) {
        accessGuard.requireFarmOwner(currentUserId, farmId);

        FarmMember member = farmMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên ID: " + memberId));

        if (!member.getFarm().getId().equals(farmId)) {
            throw new BusinessRuleException("Thành viên không thuộc trang trại này");
        }

        if (member.isOwner()) {
            long activeOwners = farmMemberRepository.countByFarmIdAndFarmRoleAndStatus(farmId, "owner", "active");
            if (activeOwners <= 1) {
                throw new BusinessRuleException("Không thể ngừng hoạt động Chủ trang trại duy nhất của trang trại");
            }
        }

        member.setMembershipStatus("left");
        farmMemberRepository.save(member);

        List<StaffZoneAssignment> activeAssignments = assignmentRepository.findActiveAssignmentsByMemberId(memberId);
        for (StaffZoneAssignment assignment : activeAssignments) {
            assignment.setEndedAt(Instant.now());
            assignmentRepository.save(assignment);
        }
    }

    @Override
    public void deleteStaff(Integer currentUserId, Integer farmId, Integer memberId) {
        accessGuard.requireFarmOwner(currentUserId, farmId);

        FarmMember member = farmMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên ID: " + memberId));

        if (!member.getFarm().getId().equals(farmId)) {
            throw new BusinessRuleException("Thành viên không thuộc trang trại này");
        }

        if (member.isOwner()) {
            long activeOwners = farmMemberRepository.countByFarmIdAndFarmRoleAndStatus(farmId, "owner", "active");
            if (activeOwners <= 1) {
                throw new BusinessRuleException("Không thể xóa Chủ trang trại duy nhất của trang trại");
            }
        }

        List<StaffZoneAssignment> activeAssignments = assignmentRepository.findActiveAssignmentsByMemberId(memberId);
        for (StaffZoneAssignment assignment : activeAssignments) {
            assignment.setEndedAt(Instant.now());
            assignmentRepository.save(assignment);
        }

        try {
            farmMemberRepository.delete(member);
            farmMemberRepository.flush();
        } catch (Exception ex) {
            member.setMembershipStatus("left");
            farmMemberRepository.save(member);
        }
    }

        @Override
    @Transactional(readOnly = true)
    public List<CandidateUserRespDTO> getCandidateUsers(Integer currentUserId, Integer farmId, String keyword) {
        accessGuard.requireFarmOwner(currentUserId, farmId);

        List<FarmMember> existingMembers = farmMemberRepository.findByFarmId(farmId);
        Set<Integer> excludedUserIds = existingMembers.stream()
                .filter(m -> "active".equalsIgnoreCase(m.getMembershipStatus()) || "invited".equalsIgnoreCase(m.getMembershipStatus()))
                .map(m -> m.getUser().getId())
                .collect(Collectors.toSet());
        excludedUserIds.add(currentUserId);

        String search = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        Page<User> usersPage = userRepository.searchUsers("active", search, PageRequest.of(0, 30, Sort.by("fullName").ascending()));

        return usersPage.getContent().stream()
                .filter(u -> !excludedUserIds.contains(u.getId()))
                .map(u -> new CandidateUserRespDTO(
                        u.getId(),
                        u.getFullName(),
                        u.getEmail(),
                        u.getPhone(),
                        u.getAvatarUrl(),
                        u.getSystemRole()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FarmInvitationRespDTO> getPendingInvitations(Integer currentUserId) {
        List<FarmMember> invitations = farmMemberRepository.findByUserIdAndStatus(currentUserId, "invited");

        return invitations.stream().map(inv -> {
            Farm farm = inv.getFarm();
            String ownerName = farmMemberRepository.findByFarmIdAndStatus(farm.getId(), "active").stream()
                    .filter(FarmMember::isOwner)
                    .map(m -> m.getUser().getFullName())
                    .findFirst()
                    .orElse("Chủ trang trại");

            return new FarmInvitationRespDTO(
                    inv.getId(),
                    farm.getId(),
                    farm.getFarmName(),
                    farm.getAddress(),
                    ownerName,
                    inv.getMemberRole(),
                    inv.getJobTitle(),
                    inv.getJoinedAt()
            );
        }).toList();
    }

    @Override
    public void respondInvitation(Integer currentUserId, Integer memberId, String action) {
        FarmMember member = farmMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lời mời với ID: " + memberId));

        if (!member.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Bạn không có quyền phản hồi lời mời của tài khoản khác");
        }

        if (!"invited".equalsIgnoreCase(member.getMembershipStatus())) {
            throw new BusinessRuleException("Lời mời này không ở trạng thái chờ phản hồi (Trạng thái hiện tại: " + member.getMembershipStatus() + ")");
        }

        if ("approve".equalsIgnoreCase(action)) {
            member.setMembershipStatus("active");
            member.setJoinedAt(Instant.now());
            farmMemberRepository.save(member);
        } else if ("reject".equalsIgnoreCase(action)) {
            member.setMembershipStatus("left");
            farmMemberRepository.save(member);
        } else {
            throw new BusinessRuleException("Hành động không hợp lệ: " + action + ". Chỉ chấp nhận 'approve' hoặc 'reject'.");
        }
    }

    private final FarmStaffMapper farmStaffMapper = new FarmStaffMapper();

    private FarmMemberRespDTO toResponse(FarmMember member, Integer activeAssignmentCount) {
        int count = (activeAssignmentCount != null)
                ? activeAssignmentCount
                : assignmentRepository.findByFarmMemberIdAndEndedAtIsNull(member.getId()).size();
        return farmStaffMapper.toStaffMemberResponse(member, count);
    }
}
