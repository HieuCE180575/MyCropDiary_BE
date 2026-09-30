package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.cultivation.CreateMaterialUsageRequest;
import com.mycropdiary.api.dto.cultivation.MaterialUsageResponse;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.ProductionArea;
import com.mycropdiary.api.entity.cultivation.*;

import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ForbiddenException;
import com.mycropdiary.api.exception.ResourceNotFoundException;

import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.StaffAreaAssignmentRepository;
import com.mycropdiary.api.repository.cultivation.*;

import com.mycropdiary.api.service.cultivation.MaterialUsageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service implementation cho nghiệp vụ nhật ký sử dụng vật tư (UC-25).
 */
@Service
@Transactional(readOnly = true)
public class MaterialUsageServiceImpl implements MaterialUsageService {

    private final MaterialUsageRepository materialUsageRepository;
    private final MaterialRepository materialRepository;
    private final InputPurchaseDetailRepository inputPurchaseDetailRepository;
    private final FarmingActivityRepository farmingActivityRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final StaffAreaAssignmentRepository staffAreaAssignmentRepository;

    public MaterialUsageServiceImpl(
            MaterialUsageRepository materialUsageRepository,
            MaterialRepository materialRepository,
            InputPurchaseDetailRepository inputPurchaseDetailRepository,
            FarmingActivityRepository farmingActivityRepository,
            FarmMemberRepository farmMemberRepository,
            StaffAreaAssignmentRepository staffAreaAssignmentRepository) {
        this.materialUsageRepository = materialUsageRepository;
        this.materialRepository = materialRepository;
        this.inputPurchaseDetailRepository = inputPurchaseDetailRepository;
        this.farmingActivityRepository = farmingActivityRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.staffAreaAssignmentRepository = staffAreaAssignmentRepository;
    }

    @Override
    @Transactional
    public MaterialUsageResponse createMaterialUsage(Long userId, CreateMaterialUsageRequest request) {
        // 1. Tìm FarmingActivity
        FarmingActivity activity = farmingActivityRepository.findById(request.farmingActivityId())
                .orElseThrow(() -> new ResourceNotFoundException("Hoạt động canh tác không tồn tại."));

        // 2. Validate Authorization & lấy active FarmMember
        FarmMember recordedByMember = validateAndGetActiveMember(userId, activity);

        // 3. Kiểm tra trạng thái mùa vụ (CropSeason)
        CropSeason season = activity.getCropSeason();
        if (season == null) {
            throw new BadRequestException("Hoạt động canh tác không thuộc mùa vụ hợp lệ.");
        }
        String seasonStatus = season.getStatus();
        if (seasonStatus != null && ("COMPLETED".equalsIgnoreCase(seasonStatus)
                || "CANCELLED".equalsIgnoreCase(seasonStatus)
                || "ARCHIVED".equalsIgnoreCase(seasonStatus))) {
            throw new BadRequestException("Không thể ghi nhận sử dụng vật tư cho mùa vụ đã đóng hoặc đã hủy.");
        }

        // 4. Tìm Material
        Material material = materialRepository.findById(request.materialId())
                .orElseThrow(() -> new ResourceNotFoundException("Vật tư không tồn tại."));

        // 5. Validate Material active
        if (Boolean.FALSE.equals(material.getIsActive())) {
            throw new BadRequestException("Vật tư đã bị vô hiệu hóa.");
        }

        // 6. Validate Material belongs to the same Farm
        Farm farm = season.getFarm();
        if (farm == null || material.getFarm() == null || !material.getFarm().getId().equals(farm.getId())) {
            throw new BadRequestException("Vật tư không thuộc trang trại của hoạt động canh tác này.");
        }

        // 7. Validate Quantity > 0
        if (request.quantity() == null || request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Số lượng sử dụng phải lớn hơn 0.");
        }

        // 8. Validate Unit (must not be blank & must match Material.unit)
        if (request.unit() == null || request.unit().trim().isEmpty()) {
            throw new BadRequestException("Đơn vị tính không được để trống.");
        }
        if (material.getUnit() != null && !material.getUnit().equalsIgnoreCase(request.unit().trim())) {
            throw new BadRequestException("Đơn vị tính (" + request.unit().trim() + ") không khớp với đơn vị tính của vật tư (" + material.getUnit() + ").");
        }

        // 9. Validate Pesticide Safety Interval
        if (request.safetyIntervalDays() != null && request.safetyIntervalDays() < 0) {
            throw new BadRequestException("Thời gian cách ly phải >= 0.");
        }
        if (material.getMaterialType() == MaterialType.PESTICIDE && request.safetyIntervalDays() == null) {
            throw new BadRequestException("Thời gian cách ly (SafetyIntervalDays) là bắt buộc đối với loại vật tư PESTICIDE.");
        }

        // 10. Validate InputPurchaseDetail nếu có
        InputPurchaseDetail inputPurchaseDetail = null;
        if (request.inputPurchaseDetailId() != null) {
            inputPurchaseDetail = inputPurchaseDetailRepository.findById(request.inputPurchaseDetailId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chi tiết hóa đơn mua vật tư không tồn tại."));

            if (!inputPurchaseDetail.getMaterial().getId().equals(material.getId())) {
                throw new BadRequestException("Chi tiết hóa đơn mua vật tư không thuộc về vật tư đã chọn.");
            }
        }

        // 11. Tạo mới MaterialUsage
        MaterialUsage usage = new MaterialUsage();
        usage.setFarmingActivity(activity);
        usage.setMaterial(material);
        usage.setInputPurchaseDetail(inputPurchaseDetail);
        usage.setRecordedByMember(recordedByMember);
        usage.setUsedAt(request.usedAt());
        usage.setQuantity(request.quantity());
        usage.setUnit(request.unit().trim());
        usage.setDosage(request.dosage() != null ? request.dosage().trim() : null);
        usage.setMethod(request.method() != null ? request.method().trim() : null);
        usage.setSafetyIntervalDays(request.safetyIntervalDays());
        usage.setNotes(request.notes() != null ? request.notes().trim() : null);

        MaterialUsage savedUsage = materialUsageRepository.save(usage);
        return mapToResponse(savedUsage);
    }

    @Override
    public PageResponse<MaterialUsageResponse> getMaterialUsagesByActivity(Long userId, Long farmingActivityId, Pageable pageable) {
        FarmingActivity activity = farmingActivityRepository.findById(farmingActivityId)
                .orElseThrow(() -> new ResourceNotFoundException("Hoạt động canh tác không tồn tại."));

        validateAndGetActiveMember(userId, activity);

        Page<MaterialUsage> usagesPage = materialUsageRepository.findByFarmingActivityId(farmingActivityId, pageable);
        return PageResponse.map(usagesPage, this::mapToResponse);
    }

    @Override
    public MaterialUsageResponse getMaterialUsageById(Long userId, Long materialUsageId) {
        MaterialUsage usage = materialUsageRepository.findById(materialUsageId)
                .orElseThrow(() -> new ResourceNotFoundException("Bản ghi sử dụng vật tư không tồn tại."));

        validateAndGetActiveMember(userId, usage.getFarmingActivity());

        return mapToResponse(usage);
    }

    /**
     * Helper validate active FarmMember & Phân quyền theo FarmRole / StaffAreaAssignment (Tương tự Task 7).
     */
    private FarmMember validateAndGetActiveMember(Long userId, FarmingActivity activity) {
        CropSeason season = activity.getCropSeason();
        Farm farm = season.getFarm();
        Plot plot = season.getPlot();
        ProductionArea productionArea = plot != null ? plot.getProductionArea() : null;

        FarmMember member = farmMemberRepository.findByFarmIdAndUserIdAndStatus(farm.getId(), userId, "ACTIVE")
                .orElseThrow(() -> new ForbiddenException("Bạn không có quyền truy cập hoặc ghi nhật ký cho mùa vụ của trang trại này."));

        String role = member.getFarmRole();
        if ("OWNER".equalsIgnoreCase(role)) {
            return member;
        } else if ("STAFF".equalsIgnoreCase(role)) {
            if (productionArea == null) {
                throw new ForbiddenException("Thửa đất của mùa vụ không thuộc vùng sản xuất hợp lệ.");
            }
            boolean isAssigned = staffAreaAssignmentRepository
                    .existsByFarmMemberIdAndProductionAreaIdAndActiveTrue(member.getId(), productionArea.getId());

            if (!isAssigned) {
                throw new ForbiddenException("Nhân viên chưa được phân công quản lý vùng sản xuất chứa mùa vụ này.");
            }
            return member;
        } else {
            throw new ForbiddenException("Quyền trang trại không hợp lệ: " + role);
        }
    }

    private MaterialUsageResponse mapToResponse(MaterialUsage usage) {
        String recorderName = usage.getRecordedByMember() != null && usage.getRecordedByMember().getUser() != null
                ? usage.getRecordedByMember().getUser().getFullName()
                : null;

        return new MaterialUsageResponse(
                usage.getId(),
                usage.getFarmingActivity().getId(),
                usage.getFarmingActivity().getActivityName(),
                usage.getFarmingActivity().getCropSeason().getId(),
                usage.getMaterial().getId(),
                usage.getMaterial().getMaterialCode(),
                usage.getMaterial().getMaterialName(),
                usage.getMaterial().getMaterialType().name(),
                usage.getInputPurchaseDetail() != null ? usage.getInputPurchaseDetail().getId() : null,
                usage.getRecordedByMember().getId(),
                recorderName,
                usage.getUsedAt(),
                usage.getQuantity(),
                usage.getUnit(),
                usage.getDosage(),
                usage.getMethod(),
                usage.getSafetyIntervalDays(),
                usage.getNotes()
        );
    }
}
