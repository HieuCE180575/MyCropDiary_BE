package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.admin.UpdateUserStatusReq;
import com.mycropdiary.api.dto.admin.UserResp;
import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.StaffZoneAssignment;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.StaffZoneAssignmentRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.service.admin.AdminUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final StaffZoneAssignmentRepository staffZoneAssignmentRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public AdminUserServiceImpl(
            UserRepository userRepository,
            FarmMemberRepository farmMemberRepository,
            StaffZoneAssignmentRepository staffZoneAssignmentRepository
    ) {
        this.userRepository = userRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.staffZoneAssignmentRepository = staffZoneAssignmentRepository;
    }

    public AdminUserServiceImpl(UserRepository userRepository) {
        this(userRepository, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResp<UserResp> getUsers(String status, String search, Pageable pageable) {
        String cleanStatus = (status != null && !status.isBlank()) ? status.trim().toLowerCase() : null;
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<User> page = userRepository.searchUsers(cleanStatus, cleanSearch, pageable);
        Map<Integer, List<FarmMember>> membersByUser = farmMemberRepository == null ? Collections.emptyMap() : farmMemberRepository.findAll().stream()
                .filter(m -> m.getUser() != null && m.getUser().getId() != null && m.isActive())
                .collect(Collectors.groupingBy(m -> m.getUser().getId()));
        Map<Integer, List<StaffZoneAssignment>> zonesByUser = staffZoneAssignmentRepository == null ? Collections.emptyMap() : staffZoneAssignmentRepository.findAll().stream()
                .filter(a -> a.getFarmMember() != null && a.getFarmMember().getUser() != null && a.getFarmMember().getUser().getId() != null && a.isActive())
                .collect(Collectors.groupingBy(a -> a.getFarmMember().getUser().getId()));

        return PageResp.from(page.map(u -> toEnrichedUserResp(
                u,
                membersByUser.getOrDefault(u.getId(), Collections.emptyList()),
                zonesByUser.getOrDefault(u.getId(), Collections.emptyList())
        )));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResp getUserById(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với mã ID: " + userId));
        return enrichSingleUser(user);
    }

    @Override
    public UserResp updateUserStatus(Integer adminUserId, Integer targetUserId, UpdateUserStatusReq request) {
        String newStatus = request.status().trim().toLowerCase();

        if (adminUserId != null && adminUserId.equals(targetUserId) && "locked".equalsIgnoreCase(newStatus)) {
            throw new BadRequestException("Quản trị viên không thể tự khóa tài khoản của chính mình.");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với mã ID: " + targetUserId));

        user.setAccountStatus(newStatus);
        User savedUser = userRepository.save(user);

        return enrichSingleUser(savedUser);
    }

    @Override
    public UserResp updateUserProfile(Integer adminUserId, Integer targetUserId, Map<String, Object> request) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với mã ID: " + targetUserId));

        if (request.containsKey("fullName") && request.get("fullName") != null) {
            String fullName = String.valueOf(request.get("fullName")).trim();
            if (!fullName.isEmpty()) {
                user.setFullName(fullName);
            }
        }
        if (request.containsKey("phone")) {
            String phone = request.get("phone") != null ? String.valueOf(request.get("phone")).trim() : null;
            user.setPhone(phone);
        }
        if (request.containsKey("systemRole") && request.get("systemRole") != null) {
            String role = String.valueOf(request.get("systemRole")).trim().toLowerCase();
            if (adminUserId != null && adminUserId.equals(targetUserId) && !"admin".equalsIgnoreCase(role)) {
                throw new BadRequestException("Quản trị viên không thể tự hạ quyền của chính mình.");
            }
            user.setSystemRole(role);
        }
        if (request.containsKey("accountStatus") && request.get("accountStatus") != null) {
            String status = String.valueOf(request.get("accountStatus")).trim().toLowerCase();
            if (adminUserId != null && adminUserId.equals(targetUserId) && "locked".equalsIgnoreCase(status)) {
                throw new BadRequestException("Quản trị viên không thể tự khóa tài khoản của chính mình.");
            }
            user.setAccountStatus(status);
        }

        User saved = userRepository.save(user);
        return enrichSingleUser(saved);
    }

    private UserResp enrichSingleUser(User user) {
        List<FarmMember> memberships = farmMemberRepository == null ? Collections.emptyList() : farmMemberRepository.findAll().stream()
                .filter(m -> m.getUser() != null && user.getId().equals(m.getUser().getId()) && m.isActive())
                .toList();
        List<StaffZoneAssignment> zoneAssignments = staffZoneAssignmentRepository == null ? Collections.emptyList() : staffZoneAssignmentRepository.findAll().stream()
                .filter(a -> a.getFarmMember() != null && a.getFarmMember().getUser() != null && user.getId().equals(a.getFarmMember().getUser().getId()) && a.isActive())
                .toList();
        return toEnrichedUserResp(user, memberships, zoneAssignments);
    }

    private UserResp toEnrichedUserResp(User user, List<FarmMember> memberships, List<StaffZoneAssignment> zoneAssignments) {
        String farmsSummary = memberships.isEmpty()
                ? "Chưa tham gia trang trại"
                : memberships.stream()
                        .filter(m -> m.getFarm() != null)
                        .map(m -> {
                            String roleLabel = m.isOwner() ? "Chủ trại" : "Nhân sự";
                            return m.getFarm().getFarmName() + " (" + roleLabel + ")";
                        })
                        .collect(Collectors.joining(", "));

        String zonesSummary = zoneAssignments.isEmpty()
                ? "Chưa phụ trách phân khu"
                : zoneAssignments.stream()
                        .filter(a -> a.getZone() != null)
                        .map(a -> a.getZone().getZoneName() + (a.getZone().getFarm() != null ? " - " + a.getZone().getFarm().getFarmName() : ""))
                        .collect(Collectors.joining(", "));

        return UserResp.fromEntity(user, farmsSummary, zonesSummary);
    }
}
