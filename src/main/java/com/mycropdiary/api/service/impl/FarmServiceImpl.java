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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Lớp triển khai các dịch vụ nghiệp vụ cốt lõi cho module Farm Core.
 * Quản lý trang trại, thành viên, vùng sản xuất, phân quyền và kiểm soát Business Rules.
 */
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

    /**
     * BR-03: Kiểm tra tính hợp lệ của tọa độ địa lý (Latitude & Longitude).
     * Yêu cầu hoặc cùng null, hoặc cùng có giá trị nằm trong dải cho phép (-90..90, -180..180).
     */
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

    /**
     * BR-04: Kiểm tra tổng diện tích các vùng sản xuất active không được vượt quá diện tích tổng của trang trại.
     */
    private void validateProductionAreaTotalSize(Long farmId, BigDecimal farmTotalArea, BigDecimal newAreaSize, Long excludeAreaId) {
        if (farmTotalArea == null || newAreaSize == null) {
            return;
        }
        BigDecimal existingSum = productionAreaRepository.sumActiveAreaM2ByFarmId(farmId, excludeAreaId);
        BigDecimal projectedTotal = existingSum.add(newAreaSize);
        if (projectedTotal.compareTo(farmTotalArea) > 0) {
            throw new IllegalArgumentException("Total production area (" + projectedTotal + " m2) exceeds farm total area (" + farmTotalArea + " m2)");
        }
    }

    /**
     * Tạo trang trại mới và tự động thiết lập quyền OWNER cho người tạo.
     */
    @Override
    public FarmResponse createFarm(Long currentUserId, CreateFarmRequest request) {
        if (farmRepository.existsByFarmCode(request.farmCode())) {
            throw new IllegalArgumentException("Farm code already exists: " + request.farmCode());
        }

        validateCoordinates(request.latitude(), request.longitude());

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

        // Người tạo tự động trở thành Farm OWNER
        FarmMember ownerMember = new FarmMember(farm, currentUser, "OWNER", "Farm Owner", LocalDate.now(), "ACTIVE");
        farmMemberRepository.save(ownerMember);

        return farmMapper.toResponse(farm, "OWNER");
    }

    /**
     * Lấy chi tiết trang trại theo ID. Kiểm tra quyền xem của thành viên trang trại.
     */
    @Override
    @Transactional(readOnly = true)
    public FarmResponse getFarmById(Long currentUserId, Long farmId) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with ID: " + farmId));

        farmAccessGuard.requireFarmMember(currentUserId, farmId);
        String role = farmAccessGuard.getUserRoleInFarm(currentUserId, farmId);

        return farmMapper.toResponse(farm, role);
    }

    /**
     * Tìm kiếm và phân trang danh sách các trang trại trong scope truy cập của người dùng.
     */
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

    /**
     * Lấy toàn bộ danh sách trang trại mà người dùng có quyền truy cập.
     */
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

    /**
     * Cập nhật thông tin trang trại. Yêu cầu quyền OWNER hoặc ADMIN.
     */
    @Override
    public FarmResponse updateFarm(Long currentUserId, Long farmId, UpdateFarmRequest request) {
        farmAccessGuard.requireOwnerOrAdmin(currentUserId, farmId);

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with ID: " + farmId));

        validateCoordinates(request.latitude(), request.longitude());

        if (request.totalAreaM2() != null) {
            BigDecimal activeAreasSum = productionAreaRepository.sumActiveAreaM2ByFarmId(farmId, null);
            if (activeAreasSum.compareTo(request.totalAreaM2()) > 0) {
                throw new IllegalArgumentException("Cannot reduce farm area to " + request.totalAreaM2() + " m2 because active production areas sum up to " + activeAreasSum + " m2");
            }
        }

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

    /**
     * Vô hiệu hóa (Soft delete) trang trại sang trạng thái INACTIVE.
     */
    @Override
    public void deleteFarm(Long currentUserId, Long farmId) {
        farmAccessGuard.requireOwnerOrAdmin(currentUserId, farmId);

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with ID: " + farmId));

        farm.setStatus("INACTIVE");
        farmRepository.save(farm);
    }

    /**
     * BR-01: Thêm thành viên mới vào trang trại. Kiểm tra quy tắc duy nhất 1 Active OWNER.
     */
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

        if ("OWNER".equalsIgnoreCase(request.farmRole())) {
            boolean hasActiveOwner = farmMemberRepository.existsByFarmIdAndFarmRoleAndStatus(farmId, "OWNER", "ACTIVE");
            if (hasActiveOwner) {
                throw new IllegalStateException("Farm already has an active OWNER. Transfer ownership before assigning a new OWNER.");
            }
        }

        FarmMember member = new FarmMember(farm, targetUser, request.farmRole(), request.jobTitle(), LocalDate.now(), "ACTIVE");
        member = farmMemberRepository.save(member);

        return farmMapper.toMemberResponse(member);
    }

    /**
     * Lấy danh sách thành viên trang trại đang hoạt động.
     */
    @Override
    @Transactional(readOnly = true)
    public List<FarmMemberResponse> getFarmMembers(Long currentUserId, Long farmId) {
        farmAccessGuard.requireFarmMember(currentUserId, farmId);
        return farmMemberRepository.findByFarmIdAndStatus(farmId, "ACTIVE").stream()
                .map(farmMapper::toMemberResponse)
                .toList();
    }

    /**
     * BR-02: Vô hiệu hóa thành viên khỏi trang trại. Không cho phép xóa Active OWNER duy nhất.
     */
    @Override
    public void removeMember(Long currentUserId, Long farmId, Long memberId) {
        farmAccessGuard.requireOwnerOrAdmin(currentUserId, farmId);

        FarmMember member = farmMemberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm member not found with ID: " + memberId));

        if (!member.getFarm().getId().equals(farmId)) {
            throw new IllegalArgumentException("Member ID does not belong to Farm ID: " + farmId);
        }

        if ("OWNER".equalsIgnoreCase(member.getFarmRole()) && "ACTIVE".equalsIgnoreCase(member.getStatus())) {
            long activeOwners = farmMemberRepository.countByFarmIdAndFarmRoleAndStatus(farmId, "OWNER", "ACTIVE");
            if (activeOwners <= 1) {
                throw new IllegalStateException("Cannot deactivate the sole active OWNER of the farm. Transfer ownership first.");
            }
        }

        member.setStatus("INACTIVE");
        member.setLeftAt(LocalDate.now());
        farmMemberRepository.save(member);
    }

    /**
     * BR-06: Phân công nhân viên (STAFF) quản lý Vùng sản xuất. Kiểm tra ngày bắt đầu và kết thúc.
     */
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

        LocalDate startDate = request.startDate() != null ? request.startDate() : LocalDate.now();
        if (request.endDate() != null && request.endDate().isBefore(startDate)) {
            throw new IllegalArgumentException("Assignment end date cannot be before start date");
        }

        FarmMember assigner = farmMemberRepository.findByFarmIdAndUserId(farmId, currentUserId).orElse(null);

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

    /**
     * BR-04 & BR-05: Tạo Vùng sản xuất mới trong trang trại. Kiểm tra mã duy nhất và tổng diện tích.
     */
    @Override
    public ProductionAreaResponse createProductionArea(Long currentUserId, Long farmId, CreateProductionAreaRequest request) {
        farmAccessGuard.requireOwnerOrAdmin(currentUserId, farmId);

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm not found with ID: " + farmId));

        if (productionAreaRepository.existsByFarmIdAndAreaCode(farmId, request.areaCode())) {
            throw new IllegalArgumentException("Production area code already exists in this farm: " + request.areaCode());
        }

        validateProductionAreaTotalSize(farmId, farm.getTotalAreaM2(), request.areaM2(), null);

        ProductionArea area = new ProductionArea(farm, request.areaCode(), request.areaName(), request.areaM2(), request.description(), "ACTIVE");
        area = productionAreaRepository.save(area);

        return farmMapper.toProductionAreaResponse(area);
    }

    /**
     * BR-07: Lấy danh sách các vùng sản xuất. STAFF chỉ nhìn thấy các vùng được phân công; OWNER/ADMIN xem tất cả.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProductionAreaResponse> getProductionAreas(Long currentUserId, Long farmId) {
        FarmMember member = farmAccessGuard.requireFarmMember(currentUserId, farmId);
        String role = farmAccessGuard.getUserRoleInFarm(currentUserId, farmId);

        if ("OWNER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
            return productionAreaRepository.findByFarmIdAndStatus(farmId, "ACTIVE").stream()
                    .map(farmMapper::toProductionAreaResponse)
                    .toList();
        }

        // Với Staff: chỉ lấy danh sách các vùng được phân công trong StaffAreaAssignment
        List<StaffAreaAssignment> assignments = staffAreaAssignmentRepository.findByFarmMemberIdAndActiveTrue(member.getId());
        return assignments.stream()
                .map(StaffAreaAssignment::getProductionArea)
                .filter(area -> "ACTIVE".equalsIgnoreCase(area.getStatus()))
                .map(farmMapper::toProductionAreaResponse)
                .toList();
    }
}
