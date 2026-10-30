package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.staff.AddStaffReq;
import com.mycropdiary.api.dto.staff.FarmMemberResp;
import com.mycropdiary.api.dto.staff.UpdateStaffReq;
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
import com.mycropdiary.api.service.FarmStaffService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
    public List<FarmMemberResp> getStaffList(Integer currentUserId, Integer farmId, String status, String keyword) {
        accessGuard.requireFarmAccess(currentUserId, farmId);

        String trimmedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String trimmedStatus = (status != null && !status.isBlank()) ? status.trim().toLowerCase() : null;

        List<FarmMember> members = farmMemberRepository.searchMembers(farmId, trimmedStatus, trimmedKeyword);
        return members.stream().map(this::toResponse).toList();
    }

    @Override
    public FarmMemberResp addStaff(Integer currentUserId, Integer farmId, AddStaffReq request) {
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

        String role = (request.farmRole() != null && !request.farmRole().isBlank()) ? request.farmRole().toLowerCase() : "member";

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

        FarmMember saved = farmMemberRepository.save(member);
        return toResponse(saved);
    }

    @Override
    public FarmMemberResp updateStaff(Integer currentUserId, Integer farmId, Integer memberId, UpdateStaffReq request) {
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
            member.setMemberRole(request.farmRole().toLowerCase());
        } else if (request.farmRole() != null) {
            member.setMemberRole(request.farmRole().toLowerCase());
        }

        if (request.status() != null && !request.status().equalsIgnoreCase(member.getMembershipStatus())) {
            if ("suspended".equalsIgnoreCase(request.status()) || "left".equalsIgnoreCase(request.status()) || "inactive".equalsIgnoreCase(request.status())) {
                deactivateStaff(currentUserId, farmId, memberId);
                return toResponse(member);
            }
            member.setMembershipStatus(request.status().toLowerCase());
        }

        FarmMember saved = farmMemberRepository.save(member);
        return toResponse(saved);
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

    private FarmMemberResp toResponse(FarmMember member) {
        List<StaffZoneAssignment> activeAssignments = assignmentRepository.findByFarmMemberIdAndEndedAtIsNull(member.getId());
        return new FarmMemberResp(
                member.getId(),
                member.getFarm().getId(),
                member.getFarm().getFarmName(),
                member.getUser().getId(),
                member.getUser().getFullName(),
                member.getUser().getEmail(),
                member.getUser().getPhone(),
                member.getMemberRole(),
                null,
                member.getJoinedAt(),
                null,
                member.getMembershipStatus(),
                activeAssignments.size()
        );
    }
}
