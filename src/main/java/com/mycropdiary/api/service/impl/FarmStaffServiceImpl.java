package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.staff.AddStaffRequest;
import com.mycropdiary.api.dto.staff.FarmMemberResponse;
import com.mycropdiary.api.dto.staff.UpdateStaffRequest;
import com.mycropdiary.api.entity.AppUser;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.StaffAreaAssignment;
import com.mycropdiary.api.exception.BusinessRuleException;
import com.mycropdiary.api.exception.ResourceConflictException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.AppUserRepository;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.StaffAreaAssignmentRepository;
import com.mycropdiary.api.service.FarmStaffService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FarmStaffServiceImpl implements FarmStaffService {
    private final FarmMemberRepository farmMemberRepository;
    private final FarmRepository farmRepository;
    private final AppUserRepository appUserRepository;
    private final StaffAreaAssignmentRepository assignmentRepository;
    private final FarmAccessGuard accessGuard;

    public FarmStaffServiceImpl(FarmMemberRepository farmMemberRepository,
                                FarmRepository farmRepository,
                                AppUserRepository appUserRepository,
                                StaffAreaAssignmentRepository assignmentRepository,
                                FarmAccessGuard accessGuard) {
        this.farmMemberRepository = farmMemberRepository;
        this.farmRepository = farmRepository;
        this.appUserRepository = appUserRepository;
        this.assignmentRepository = assignmentRepository;
        this.accessGuard = accessGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FarmMemberResponse> getStaffList(Long currentUserId, Long farmId, String status, String keyword) {
        // Kiểm tra quyền: Người dùng phải là thành viên hợp lệ của farm
        accessGuard.requireFarmAccess(currentUserId, farmId);

        String trimmedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String trimmedStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : null;

        List<FarmMember> members = farmMemberRepository.searchMembers(farmId, trimmedStatus, trimmedKeyword);
        return members.stream().map(this::toResponse).toList();
    }

    @Override
    public FarmMemberResponse addStaff(Long currentUserId, Long farmId, AddStaffRequest request) {
        // Business Rule: Chỉ OWNER của farm mới có quyền thêm nhân viên
        accessGuard.requireFarmOwner(currentUserId, farmId);

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trang trại ID: " + farmId));

        // Tìm tài khoản AppUser theo email hoặc userId
        AppUser user;
        if (request.email() != null && !request.email().isBlank()) {
            user = appUserRepository.findByEmailIgnoreCase(request.email().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với email: " + request.email()));
        } else if (request.userId() != null) {
            user = appUserRepository.findById(request.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với ID: " + request.userId()));
        } else {
            throw new BusinessRuleException("Vui lòng cung cấp email hoặc userId của nhân viên cần thêm");
        }

        // Business Rule: Kiểm tra trạng thái tài khoản
        if (!"ACTIVE".equalsIgnoreCase(user.getAccountStatus())) {
            throw new BusinessRuleException("Tài khoản người dùng chưa được kích hoạt hoặc đang bị khóa (trạng thái: " + user.getAccountStatus() + ")");
        }

        String role = (request.farmRole() != null && !request.farmRole().isBlank()) ? request.farmRole().toUpperCase() : "STAFF";

        // Business Rule: Kiểm tra người dùng đã tồn tại trong farm chưa
        Optional<FarmMember> existingMemberOpt = farmMemberRepository.findByFarmIdAndUserId(farmId, user.getId());
        FarmMember member;
        if (existingMemberOpt.isPresent()) {
            member = existingMemberOpt.get();
            if ("ACTIVE".equalsIgnoreCase(member.getStatus())) {
                throw new ResourceConflictException("Người dùng (" + user.getEmail() + ") đã là thành viên ACTIVE trong trang trại này");
            }
            // Tái kích hoạt thành viên từng ngừng hoạt động
            member.setStatus("ACTIVE");
            member.setFarmRole(role);
            member.setJobTitle(request.jobTitle());
            member.setJoinedAt(LocalDate.now());
            member.setLeftAt(null);
        } else {
            member = new FarmMember(farm, user, role, request.jobTitle(), "ACTIVE");
        }

        FarmMember saved = farmMemberRepository.save(member);
        return toResponse(saved);
    }

    @Override
    public FarmMemberResponse updateStaff(Long currentUserId, Long farmId, Long memberId, UpdateStaffRequest request) {
        // Business Rule: Chỉ OWNER mới có quyền cập nhật nhân viên
        accessGuard.requireFarmOwner(currentUserId, farmId);

        FarmMember member = farmMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên ID: " + memberId));

        if (!member.getFarm().getId().equals(farmId)) {
            throw new BusinessRuleException("Thành viên không thuộc trang trại này");
        }

        // Business Rule: Kiểm tra không cho phép hạ quyền nếu là Owner duy nhất còn lại
        if (request.farmRole() != null && "STAFF".equalsIgnoreCase(request.farmRole()) && member.isOwner()) {
            long activeOwners = farmMemberRepository.countByFarmIdAndFarmRoleAndStatus(farmId, "OWNER", "ACTIVE");
            if (activeOwners <= 1) {
                throw new BusinessRuleException("Không thể hạ quyền Chủ trang trại duy nhất còn lại của trang trại");
            }
            member.setFarmRole("STAFF");
        } else if (request.farmRole() != null) {
            member.setFarmRole(request.farmRole().toUpperCase());
        }

        if (request.jobTitle() != null) {
            member.setJobTitle(request.jobTitle());
        }

        if (request.status() != null && !request.status().equalsIgnoreCase(member.getStatus())) {
            if ("INACTIVE".equalsIgnoreCase(request.status())) {
                deactivateStaff(currentUserId, farmId, memberId);
                return toResponse(member);
            }
            member.setStatus(request.status().toUpperCase());
        }

        FarmMember saved = farmMemberRepository.save(member);
        return toResponse(saved);
    }

    @Override
    public void deactivateStaff(Long currentUserId, Long farmId, Long memberId) {
        // Business Rule: Chỉ OWNER mới có quyền ngừng hoạt động nhân viên
        accessGuard.requireFarmOwner(currentUserId, farmId);

        FarmMember member = farmMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên ID: " + memberId));

        if (!member.getFarm().getId().equals(farmId)) {
            throw new BusinessRuleException("Thành viên không thuộc trang trại này");
        }

        // Business Rule: Không thể vô hiệu hóa Chủ trang trại duy nhất
        if (member.isOwner()) {
            long activeOwners = farmMemberRepository.countByFarmIdAndFarmRoleAndStatus(farmId, "OWNER", "ACTIVE");
            if (activeOwners <= 1) {
                throw new BusinessRuleException("Không thể ngừng hoạt động Chủ trang trại duy nhất của trang trại");
            }
        }

        // Cập nhật trạng thái ngừng hoạt động
        member.setStatus("INACTIVE");
        member.setLeftAt(LocalDate.now());
        farmMemberRepository.save(member);

        // Business Rule: Tự động hủy/đóng tất cả các phân công khu vực đang active của nhân viên này
        List<StaffAreaAssignment> activeAssignments = assignmentRepository.findActiveAssignmentsByMemberId(memberId);
        for (StaffAreaAssignment assignment : activeAssignments) {
            assignment.setIsActive(false);
            assignment.setEndDate(LocalDate.now());
            assignmentRepository.save(assignment);
        }
    }

    private FarmMemberResponse toResponse(FarmMember member) {
        List<StaffAreaAssignment> activeAssignments = assignmentRepository.findByFarmMemberIdAndIsActiveTrue(member.getId());
        return new FarmMemberResponse(
                member.getId(),
                member.getFarm().getId(),
                member.getFarm().getFarmName(),
                member.getUser().getId(),
                member.getUser().getFullName(),
                member.getUser().getEmail(),
                member.getUser().getPhoneNumber(),
                member.getFarmRole(),
                member.getJobTitle(),
                member.getJoinedAt(),
                member.getLeftAt(),
                member.getStatus(),
                activeAssignments.size()
        );
    }
}
