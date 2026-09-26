package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.farm.*;
import com.mycropdiary.api.entity.AppUser;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.ProductionArea;
import com.mycropdiary.api.entity.StaffAreaAssignment;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.mapper.FarmMapper;
import com.mycropdiary.api.repository.AppUserRepository;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.ProductionAreaRepository;
import com.mycropdiary.api.repository.StaffAreaAssignmentRepository;
import com.mycropdiary.api.service.FarmService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class FarmServiceImpl implements FarmService {
    private final FarmRepository farmRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final AppUserRepository appUserRepository;
    private final ProductionAreaRepository productionAreaRepository;
    private final StaffAreaAssignmentRepository staffAreaAssignmentRepository;
    private final FarmMapper farmMapper;
    private final FarmAccessGuard farmAccessGuard;

    public FarmServiceImpl(FarmRepository farmRepository,
                           FarmMemberRepository farmMemberRepository,
                           AppUserRepository appUserRepository,
                           ProductionAreaRepository productionAreaRepository,
                           StaffAreaAssignmentRepository staffAreaAssignmentRepository,
                           FarmMapper farmMapper,
                           FarmAccessGuard farmAccessGuard) {
        this.farmRepository = farmRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.appUserRepository = appUserRepository;
        this.productionAreaRepository = productionAreaRepository;
        this.staffAreaAssignmentRepository = staffAreaAssignmentRepository;
        this.farmMapper = farmMapper;
        this.farmAccessGuard = farmAccessGuard;
    }

    @Override
    public FarmResponse createFarm(Long currentUserId, CreateFarmRequest request) {
        if (farmRepository.existsByFarmCode(request.farmCode())) {
            throw new IllegalArgumentException("Farm code already exists: " + request.farmCode());
        }

        AppUser currentUser = appUserRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        Farm farm = new Farm(
                request.farmCode(),
                request.farmName(),
                request.addressLine(),
                request.province(),
                request.district(),
                request.ward(),
                request.latitude(),
                request.longitude(),
                request.totalAreaM2(),
                "ACTIVE"
        );
        farm = farmRepository.save(farm);

        // Creator automatically becomes the Farm OWNER
        FarmMember ownerMember = new FarmMember(farm, currentUser, "OWNER", "Farm Owner", LocalDate.now(), "ACTIVE");
        farmMemberRepository.save(ownerMember);

        return farmMapper.toResponse(farm, "OWNER");
    }

    @Override
    @Transactional(readOnly = true)
    public FarmResponse getFarmById(Long currentUserId, Long farmId) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with ID: " + farmId));

        farmAccessGuard.requireFarmMember(currentUserId, farmId);
        String role = farmAccessGuard.getUserRoleInFarm(currentUserId, farmId);

        return farmMapper.toResponse(farm, role);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FarmSummaryResponse> searchFarms(Long currentUserId, String keyword, String status, String province, Pageable pageable) {
        if (farmAccessGuard.isSystemAdmin(currentUserId)) {
            Page<Farm> allFarms = farmRepository.findAll(pageable);
            return PageResponse.map(allFarms, farm -> farmMapper.toSummary(farm, "ADMIN"));
        }

        Page<Farm> accessibleFarms = farmRepository.findAccessibleFarmsByUser(currentUserId, keyword, status, province, pageable);
        return PageResponse.map(accessibleFarms, farm -> {
            String role = farmAccessGuard.getUserRoleInFarm(currentUserId, farm.getId());
            return farmMapper.toSummary(farm, role);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<FarmSummaryResponse> findAllAccessibleFarms(Long currentUserId) {
        if (farmAccessGuard.isSystemAdmin(currentUserId)) {
            return farmRepository.findAll().stream()
                    .map(farm -> farmMapper.toSummary(farm, "ADMIN"))
                    .toList();
        }

        return farmRepository.findAllAccessibleFarmsByUser(currentUserId).stream()
                .map(farm -> {
                    String role = farmAccessGuard.getUserRoleInFarm(currentUserId, farm.getId());
                    return farmMapper.toSummary(farm, role);
                })
                .toList();
    }

    @Override
    public FarmResponse updateFarm(Long currentUserId, Long farmId, UpdateFarmRequest request) {
        farmAccessGuard.requireOwnerOrAdmin(currentUserId, farmId);

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with ID: " + farmId));

        farm.setFarmName(request.farmName());
        farm.setAddressLine(request.addressLine());
        farm.setProvince(request.province());
        farm.setDistrict(request.district());
        farm.setWard(request.ward());
        farm.setLatitude(request.latitude());
        farm.setLongitude(request.longitude());
        farm.setTotalAreaM2(request.totalAreaM2());

        if (request.status() != null && !request.status().isBlank()) {
            farm.setStatus(request.status());
        }

        farm = farmRepository.save(farm);
        String role = farmAccessGuard.getUserRoleInFarm(currentUserId, farmId);

        return farmMapper.toResponse(farm, role);
    }

    @Override
    public void deleteFarm(Long currentUserId, Long farmId) {
        farmAccessGuard.requireOwnerOrAdmin(currentUserId, farmId);

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with ID: " + farmId));

        farm.setStatus("INACTIVE");
        farmRepository.save(farm);
    }

    @Override
    public FarmMemberResponse addMember(Long currentUserId, Long farmId, AddFarmMemberRequest request) {
        farmAccessGuard.requireOwnerOrAdmin(currentUserId, farmId);

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with ID: " + farmId));

        AppUser targetUser = appUserRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.email()));

        if (farmMemberRepository.findByFarmIdAndUserId(farmId, targetUser.getId()).isPresent()) {
            throw new IllegalArgumentException("User is already a member of this farm");
        }

        FarmMember member = new FarmMember(farm, targetUser, request.farmRole(), request.jobTitle(), LocalDate.now(), "ACTIVE");
        member = farmMemberRepository.save(member);

        return farmMapper.toMemberResponse(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FarmMemberResponse> getFarmMembers(Long currentUserId, Long farmId) {
        farmAccessGuard.requireFarmMember(currentUserId, farmId);
        return farmMemberRepository.findByFarmIdAndStatus(farmId, "ACTIVE").stream()
                .map(farmMapper::toMemberResponse)
                .toList();
    }

    @Override
    public void removeMember(Long currentUserId, Long farmId, Long memberId) {
        farmAccessGuard.requireOwnerOrAdmin(currentUserId, farmId);

        FarmMember member = farmMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm member not found with ID: " + memberId));

        if (!member.getFarm().getId().equals(farmId)) {
            throw new IllegalArgumentException("Member ID does not belong to Farm ID: " + farmId);
        }

        member.setStatus("INACTIVE");
        member.setLeftAt(LocalDate.now());
        farmMemberRepository.save(member);
    }

    @Override
    public StaffAreaAssignmentResponse assignStaffToArea(Long currentUserId, Long farmId, Long memberId, AssignStaffAreaRequest request) {
        farmAccessGuard.requireOwnerOrAdmin(currentUserId, farmId);

        FarmMember member = farmMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm member not found with ID: " + memberId));

        if (!member.getFarm().getId().equals(farmId)) {
            throw new IllegalArgumentException("Member ID does not belong to Farm ID: " + farmId);
        }

        ProductionArea area = productionAreaRepository.findById(request.productionAreaId())
                .orElseThrow(() -> new ResourceNotFoundException("Production area not found with ID: " + request.productionAreaId()));

        if (!area.getFarm().getId().equals(farmId)) {
            throw new IllegalArgumentException("Production area does not belong to Farm ID: " + farmId);
        }

        FarmMember assigner = farmMemberRepository.findByFarmIdAndUserId(farmId, currentUserId).orElse(null);

        LocalDate startDate = request.startDate() != null ? request.startDate() : LocalDate.now();

        StaffAreaAssignment assignment = new StaffAreaAssignment(
                member,
                area,
                assigner,
                startDate,
                request.endDate(),
                true
        );

        assignment = staffAreaAssignmentRepository.save(assignment);
        return farmMapper.toAssignmentResponse(assignment);
    }
}
