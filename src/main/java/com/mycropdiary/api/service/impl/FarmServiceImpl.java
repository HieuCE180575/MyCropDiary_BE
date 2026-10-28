package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.farm.*;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.StaffZoneAssignment;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.Zone;
import com.mycropdiary.api.exception.BusinessRuleException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.mapper.FarmMapper;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.StaffZoneAssignmentRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.repository.ZoneRepository;
import com.mycropdiary.api.service.FarmService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lớp triển khai các dịch vụ nghiệp vụ cốt lõi cho module Farm Core.
 */
@Service
@Transactional
public class FarmServiceImpl implements FarmService {
    private final FarmRepository farmRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final UserRepository userRepository;
    private final ZoneRepository zoneRepository;
    private final StaffZoneAssignmentRepository staffZoneAssignmentRepository;
    private final FarmMapper farmMapper;
    private final FarmAccessGuard farmAccessGuard;

    public FarmServiceImpl(FarmRepository farmRepository,
                           FarmMemberRepository farmMemberRepository,
                           UserRepository userRepository,
                           ZoneRepository zoneRepository,
                           StaffZoneAssignmentRepository staffZoneAssignmentRepository,
                           FarmMapper farmMapper,
                           FarmAccessGuard farmAccessGuard) {
        this.farmRepository = farmRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.userRepository = userRepository;
        this.zoneRepository = zoneRepository;
        this.staffZoneAssignmentRepository = staffZoneAssignmentRepository;
        this.farmMapper = farmMapper;
        this.farmAccessGuard = farmAccessGuard;
    }

    private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null && longitude == null) {
            return;
        }
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Both latitude and longitude must be provided together");
        }
        if (latitude.doubleValue() < -90.0 || latitude.doubleValue() > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees");
        }
        if (longitude.doubleValue() < -180.0 || longitude.doubleValue() > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees");
        }
    }

    @Override
    public FarmResp createFarm(Integer currentUserId, CreateFarmReq request) {
        validateCoordinates(request.latitude(), request.longitude());

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        Farm farm = new Farm();
        farm.setFarmName(request.farmName());
        farm.setDescription(request.description());
        farm.setAddress(request.address() != null ? request.address() : "");
        farm.setLatitude(request.latitude());
        farm.setLongitude(request.longitude());
        farm.setBoundaryCoordinates(request.boundaryCoordinates());
        farm.setCoverImageUrl(request.coverImageUrl());
        farm.setCreatedBy(currentUser);
        farm.setFarmStatus("active");

        Farm savedFarm = farmRepository.save(farm);

        FarmMember ownerMember = new FarmMember(savedFarm, currentUser, "owner", "active");
        farmMemberRepository.save(ownerMember);

        return farmMapper.toResponse(savedFarm, "owner");
    }

    @Override
    @Transactional(readOnly = true)
    public FarmResp getFarmById(Integer currentUserId, Integer farmId) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found: " + farmId));

        farmAccessGuard.requireFarmMember(currentUserId, farmId);
        String userRole = farmAccessGuard.getUserRoleInFarm(currentUserId, farmId);

        return farmMapper.toResponse(farm, userRole);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResp<FarmSummaryResp> searchFarms(Integer currentUserId, String keyword, String status, String province, Pageable pageable) {
        return searchFarms(currentUserId, keyword, province, status, null, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResp<FarmSummaryResp> searchFarms(
            Integer currentUserId,
            String keyword,
            String province,
            String status,
            BigDecimal minArea,
            BigDecimal maxArea,
            Pageable pageable
    ) {
        Pageable sortedPageable = pageable;
        if (pageable.getSort().isUnsorted()) {
            sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("farmName").ascending());
        }

        Page<Farm> farmPage = farmRepository.searchAccessibleFarms(
                currentUserId, keyword, province, status, minArea, maxArea, sortedPageable
        );

        List<FarmSummaryResp> content = farmPage.getContent().stream()
                .map(farm -> {
                    String role = farmAccessGuard.getUserRoleInFarm(currentUserId, farm.getId());
                    return farmMapper.toSummary(farm, role);
                })
                .toList();

        return new PageResp<>(
                content,
                farmPage.getNumber(),
                farmPage.getSize(),
                farmPage.getTotalElements(),
                farmPage.getTotalPages(),
                farmPage.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<FarmSummaryResp> findAllAccessibleFarms(Integer currentUserId) {
        List<Farm> farms = farmRepository.findAllAccessibleFarmsByUser(currentUserId);
        return farms.stream()
                .map(farm -> {
                    String role = farmAccessGuard.getUserRoleInFarm(currentUserId, farm.getId());
                    return farmMapper.toSummary(farm, role);
                })
                .toList();
    }

    @Override
    public FarmResp updateFarm(Integer currentUserId, Integer farmId, UpdateFarmReq request) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found: " + farmId));

        farmAccessGuard.requireOwnerOrAdmin(currentUserId, farmId);
        validateCoordinates(request.latitude(), request.longitude());

        if (request.farmName() != null && !request.farmName().isBlank()) {
            farm.setFarmName(request.farmName());
        }
        if (request.description() != null) {
            farm.setDescription(request.description());
        }
        if (request.address() != null) {
            farm.setAddress(request.address());
        }
        if (request.latitude() != null) {
            farm.setLatitude(request.latitude());
        }
        if (request.longitude() != null) {
            farm.setLongitude(request.longitude());
        }
        if (request.boundaryCoordinates() != null) {
            farm.setBoundaryCoordinates(request.boundaryCoordinates());
        }
        if (request.coverImageUrl() != null) {
            farm.setCoverImageUrl(request.coverImageUrl());
        }
        if (request.status() != null && !request.status().isBlank()) {
            farm.setFarmStatus(request.status());
        }

        Farm updatedFarm = farmRepository.save(farm);
        String userRole = farmAccessGuard.getUserRoleInFarm(currentUserId, farmId);
        return farmMapper.toResponse(updatedFarm, userRole);
    }

    @Override
    public void deleteFarm(Integer currentUserId, Integer farmId) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found: " + farmId));

        farmAccessGuard.requireOwnerOrAdmin(currentUserId, farmId);
        farm.setFarmStatus("archived");
        farmRepository.save(farm);
    }

    @Override
    public FarmMemberResp addMember(Integer currentUserId, Integer farmId, AddFarmMemberReq request) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found: " + farmId));

        farmAccessGuard.requireOwnerOrAdmin(currentUserId, farmId);

        String role = request.farmRole() != null ? request.farmRole().toLowerCase() : "member";
        if ("owner".equalsIgnoreCase(role)) {
            boolean hasActiveOwner = farmMemberRepository.existsByFarmIdAndFarmRoleAndStatus(farmId, "owner", "active");
            if (hasActiveOwner) {
                throw new BusinessRuleException("Trang trại đã có chủ sở hữu (Owner) đang hoạt động");
            }
        }

        User targetUser = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.email()));

        if (farmMemberRepository.existsByFarmIdAndUserId(farmId, targetUser.getId())) {
            throw new IllegalArgumentException("User is already a member of this farm");
        }

        FarmMember newMember = new FarmMember(farm, targetUser, role, "active");
        if (request.jobTitle() != null && !request.jobTitle().isBlank()) {
            newMember.setJobTitle(request.jobTitle());
        }
        FarmMember savedMember = farmMemberRepository.save(newMember);
        savedMember.setJobTitle(newMember.getJobTitle());

        return farmMapper.toMemberResponse(savedMember);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FarmMemberResp> getFarmMembers(Integer currentUserId, Integer farmId) {
        farmAccessGuard.requireFarmMember(currentUserId, farmId);
        List<FarmMember> members = farmMemberRepository.findByFarmId(farmId);
        return members.stream()
                .map(farmMapper::toMemberResponse)
                .toList();
    }

    @Override
    public void removeMember(Integer currentUserId, Integer farmId, Integer memberId) {
        farmAccessGuard.requireOwnerOrAdmin(currentUserId, farmId);

        FarmMember member = farmMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm member not found: " + memberId));

        if (!member.getFarm().getId().equals(farmId)) {
            throw new IllegalArgumentException("Member does not belong to this farm");
        }

        if (member.isOwner()) {
            long activeOwners = farmMemberRepository.countByFarmIdAndFarmRoleAndStatus(farmId, "owner", "active");
            if (activeOwners <= 1) {
                throw new BusinessRuleException("Không thể xóa chủ trang trại duy nhất");
            }
        }

        member.setMembershipStatus("left");
        farmMemberRepository.save(member);
    }

    @Override
    public StaffZoneAssignmentResp assignStaffToZone(Integer currentUserId, Integer farmId, Integer memberId, AssignStaffZoneReq request) {
        farmAccessGuard.requireOwnerOrAdmin(currentUserId, farmId);

        FarmMember staffMember = farmMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm member not found: " + memberId));

        if (!staffMember.getFarm().getId().equals(farmId)) {
            throw new IllegalArgumentException("Staff member does not belong to this farm");
        }

        Zone zone = zoneRepository.findById(request.zoneId())
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found: " + request.zoneId()));

        if (!zone.getFarm().getId().equals(farmId)) {
            throw new IllegalArgumentException("Zone does not belong to this farm");
        }

        boolean alreadyAssigned = staffZoneAssignmentRepository.existsByFarmMemberIdAndZoneIdAndEndedAtIsNull(memberId, request.zoneId());
        if (alreadyAssigned) {
            throw new IllegalArgumentException("Staff member is already assigned to this zone");
        }

        StaffZoneAssignment assignment = new StaffZoneAssignment(staffMember, zone);
        if (request.startDate() != null) {
            assignment.setAssignedAt(request.startDate());
        }
        if (request.endDate() != null) {
            assignment.setEndedAt(request.endDate());
        }
        StaffZoneAssignment saved = staffZoneAssignmentRepository.save(assignment);

        return farmMapper.toStaffZoneAssignmentResponse(saved);
    }

    @Override
    public ZoneResp createZone(Integer currentUserId, Integer farmId, CreateZoneReq request) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found: " + farmId));

        farmAccessGuard.requireOwnerOrAdmin(currentUserId, farmId);

        String zoneName = request.zoneName();
        if (zoneRepository.existsByFarmIdAndZoneNameIgnoreCase(farmId, zoneName)) {
            throw new IllegalArgumentException("Zone name already exists in this farm: " + zoneName);
        }

        Zone zone = new Zone(farm, zoneName, request.areaValue(), request.description());
        if (request.areaUnit() != null) {
            zone.setAreaUnit(request.areaUnit());
        }
        if (request.imageUrl() != null) {
            zone.setImageUrl(request.imageUrl());
        }
        Zone savedZone = zoneRepository.save(zone);

        return farmMapper.toZoneResponse(savedZone);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ZoneResp> getZones(Integer currentUserId, Integer farmId) {
        FarmMember member = farmAccessGuard.requireFarmMember(currentUserId, farmId);
        String role = farmAccessGuard.getUserRoleInFarm(currentUserId, farmId);

        if ("owner".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role)) {
            List<Zone> zones = zoneRepository.findByFarmId(farmId);
            return zones.stream()
                    .map(farmMapper::toZoneResponse)
                    .toList();
        }

        List<StaffZoneAssignment> assignments = staffZoneAssignmentRepository.findByFarmMemberIdAndEndedAtIsNull(member.getId());
        return assignments.stream()
                .map(StaffZoneAssignment::getZone)
                .map(farmMapper::toZoneResponse)
                .toList();
    }
}
