package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.CreateMaterialUsageReq;
import com.mycropdiary.api.dto.cultivation.MaterialUsageResp;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.cultivation.*;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ForbiddenException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmMemberRepository;
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
    private final FarmingActivityRepository farmingActivityRepository;
    private final FarmMemberRepository farmMemberRepository;

    public MaterialUsageServiceImpl(
            MaterialUsageRepository materialUsageRepository,
            MaterialRepository materialRepository,
            FarmingActivityRepository farmingActivityRepository,
            FarmMemberRepository farmMemberRepository) {
        this.materialUsageRepository = materialUsageRepository;
        this.materialRepository = materialRepository;
        this.farmingActivityRepository = farmingActivityRepository;
        this.farmMemberRepository = farmMemberRepository;
    }

    @Override
    @Transactional
    public MaterialUsageResp createMaterialUsage(Integer userId, CreateMaterialUsageReq request) {
        // 1. Tìm FarmingActivity
        FarmingActivity activity = farmingActivityRepository.findById(request.farmingActivityId())
                .orElseThrow(() -> new ResourceNotFoundException("Hoạt động canh tác không tồn tại."));

        // 2. Validate Authorization & lấy active FarmMember
        FarmMember recordedByMember = validateAndGetActiveMember(userId, activity);

        // 3. Kiểm tra trạng thái mùa vụ (CropSeason)
        CropSeason season = activity.getCropSeason();
        if (season != null) {
            String seasonStatus = season.getSeasonStatus();
            if (seasonStatus != null && ("completed".equalsIgnoreCase(seasonStatus)
                    || "cancelled".equalsIgnoreCase(seasonStatus)
                    || "archived".equalsIgnoreCase(seasonStatus))) {
                throw new BadRequestException("Không thể ghi nhận sử dụng vật tư cho mùa vụ đã đóng hoặc đã hủy.");
            }
        }

        // 4. Tìm Material
        Material material = materialRepository.findById(request.materialId())
                .orElseThrow(() -> new ResourceNotFoundException("Vật tư không tồn tại."));

        // 5. Validate Material active
        if (Boolean.FALSE.equals(material.getIsActive())) {
            throw new BadRequestException("Vật tư đã bị vô hiệu hóa.");
        }

        // 6. Validate Material belongs to the same Farm
        Farm farm = season != null ? season.getFarm() : activity.getFarm();
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

        // 10. Tạo mới MaterialUsage
        MaterialUsage usage = new MaterialUsage();
        usage.setFarm(farm);
        usage.setFarmingActivity(activity);
        usage.setMaterial(material);
        usage.setUsedBy(recordedByMember);
        usage.setUsedAt(request.usedAt());
        usage.setQuantity(request.quantity());
        usage.setBlock(activity.getBlock());
        usage.setNotes(request.notes() != null ? request.notes().trim() : null);

        MaterialUsage savedUsage = materialUsageRepository.save(usage);
        return mapToResponse(savedUsage);
    }

    @Override
    public PageResp<MaterialUsageResp> getMaterialUsagesByActivity(Integer userId, Integer farmingActivityId, Pageable pageable) {
        FarmingActivity activity = farmingActivityRepository.findById(farmingActivityId)
                .orElseThrow(() -> new ResourceNotFoundException("Hoạt động canh tác không tồn tại."));

        validateAndGetActiveMember(userId, activity);

        Page<MaterialUsage> usagesPage = materialUsageRepository.findByFarmingActivityId(farmingActivityId, pageable);
        return PageResp.map(usagesPage, this::mapToResponse);
    }

    @Override
    public MaterialUsageResp getMaterialUsageById(Integer userId, Integer materialUsageId) {
        MaterialUsage usage = materialUsageRepository.findById(materialUsageId)
                .orElseThrow(() -> new ResourceNotFoundException("Bản ghi sử dụng vật tư không tồn tại."));

        validateAndGetActiveMember(userId, usage.getFarmingActivity());

        return mapToResponse(usage);
    }

    private FarmMember validateAndGetActiveMember(Integer userId, FarmingActivity activity) {
        CropSeason season = activity.getCropSeason();
        Farm farm = season != null ? season.getFarm() : activity.getFarm();

        return farmMemberRepository.findByFarmIdAndUserIdAndStatus(farm.getId(), userId, "active")
                .orElseThrow(() -> new ForbiddenException("Bạn không có quyền truy cập hoặc ghi nhật ký cho mùa vụ của trang trại này."));
    }

    private MaterialUsageResp mapToResponse(MaterialUsage usage) {
        String recorderName = usage.getUsedBy() != null && usage.getUsedBy().getUser() != null
                ? usage.getUsedBy().getUser().getFullName()
                : null;
        FarmingActivity act = usage.getFarmingActivity();
        CropSeason cs = act != null ? act.getCropSeason() : null;

        return new MaterialUsageResp(
                usage.getId(),
                act != null ? act.getId() : null,
                act != null ? act.getActivityName() : null,
                cs != null ? cs.getId() : null,
                usage.getMaterial().getId(),
                usage.getMaterial().getMaterialName(),
                usage.getMaterial().getMaterialName(),
                usage.getMaterial().getMaterialType(),
                null,
                usage.getUsedBy() != null ? usage.getUsedBy().getId() : null,
                recorderName,
                usage.getUsedAtLdt(),
                usage.getQuantity(),
                usage.getUnit(),
                usage.getDosage(),
                usage.getMethod(),
                usage.getSafetyIntervalDays(),
                usage.getNotes()
        );
    }
}
