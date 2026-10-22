package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.CreateMaterialUsageReq;
import com.mycropdiary.api.dto.cultivation.MaterialUsageResp;
import com.mycropdiary.api.dto.cultivation.UpdateMaterialUsageReq;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.cultivation.*;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ForbiddenException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.cultivation.*;
import com.mycropdiary.api.service.cultivation.MaterialUsageService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

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
    private final CropSeasonRepository cropSeasonRepository;

    public MaterialUsageServiceImpl(
            MaterialUsageRepository materialUsageRepository,
            MaterialRepository materialRepository,
            FarmingActivityRepository farmingActivityRepository,
            FarmMemberRepository farmMemberRepository,
            CropSeasonRepository cropSeasonRepository) {
        this.materialUsageRepository = materialUsageRepository;
        this.materialRepository = materialRepository;
        this.farmingActivityRepository = farmingActivityRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.cropSeasonRepository = cropSeasonRepository;
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
    @Transactional
    public MaterialUsageResp updateMaterialUsage(Integer userId, Integer materialUsageId, UpdateMaterialUsageReq request) {
        MaterialUsage usage = materialUsageRepository.findById(materialUsageId)
                .orElseThrow(() -> new ResourceNotFoundException("Bản ghi sử dụng vật tư không tồn tại."));

        FarmingActivity activity = usage.getFarmingActivity();
        if (activity == null) {
            throw new BadRequestException("Bản ghi sử dụng vật tư không liên kết với hoạt động canh tác hợp lệ.");
        }

        // 1. Validate authorization
        validateAndGetActiveMember(userId, activity);

        // 2. Validate mùa vụ chưa đóng/hủy
        CropSeason season = activity.getCropSeason();
        if (season != null) {
            String seasonStatus = season.getSeasonStatus();
            if (seasonStatus != null && ("completed".equalsIgnoreCase(seasonStatus)
                    || "cancelled".equalsIgnoreCase(seasonStatus)
                    || "archived".equalsIgnoreCase(seasonStatus))) {
                throw new BadRequestException("Không thể chỉnh sửa sử dụng vật tư cho mùa vụ đã đóng hoặc đã hủy.");
            }
        }

        Farm farm = season != null ? season.getFarm() : activity.getFarm();
        Material currentMaterial = usage.getMaterial();

        // 3. Nếu cập nhật vật tư (materialId)
        if (request.materialId() != null && (currentMaterial == null || !request.materialId().equals(currentMaterial.getId()))) {
            Material newMaterial = materialRepository.findById(request.materialId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vật tư không tồn tại."));
            if (Boolean.FALSE.equals(newMaterial.getIsActive())) {
                throw new BadRequestException("Vật tư đã bị vô hiệu hóa.");
            }
            if (farm == null || newMaterial.getFarm() == null || !newMaterial.getFarm().getId().equals(farm.getId())) {
                throw new BadRequestException("Vật tư không thuộc trang trại của hoạt động canh tác này.");
            }
            usage.setMaterial(newMaterial);
            currentMaterial = newMaterial;
        }

        // 4. Nếu cập nhật số lượng (quantity)
        if (request.quantity() != null) {
            if (request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Số lượng sử dụng phải lớn hơn 0.");
            }
            usage.setQuantity(request.quantity());
        }

        // 5. Nếu cập nhật đơn vị tính (unit)
        if (request.unit() != null && !request.unit().trim().isEmpty()) {
            if (currentMaterial != null && currentMaterial.getUnit() != null
                    && !currentMaterial.getUnit().equalsIgnoreCase(request.unit().trim())) {
                throw new BadRequestException("Đơn vị tính (" + request.unit().trim() + ") không khớp với đơn vị tính của vật tư (" + currentMaterial.getUnit() + ").");
            }
            usage.setUnit(request.unit().trim());
        }

        // 6. Nếu cập nhật thời gian sử dụng (usedAt)
        if (request.usedAt() != null) {
            usage.setUsedAt(request.usedAt());
        }

        // 7. Validate thời gian cách ly (safetyIntervalDays)
        if (request.safetyIntervalDays() != null) {
            if (request.safetyIntervalDays() < 0) {
                throw new BadRequestException("Thời gian cách ly phải >= 0.");
            }
            usage.setSafetyIntervalDays(request.safetyIntervalDays());
        }

        // 8. Cập nhật các trường bổ sung
        if (request.dosage() != null) {
            usage.setDosage(request.dosage().trim());
        }
        if (request.method() != null) {
            usage.setMethod(request.method().trim());
        }
        if (request.notes() != null) {
            usage.setNotes(request.notes().trim());
        }

        MaterialUsage savedUsage = materialUsageRepository.save(usage);
        return mapToResponse(savedUsage);
    }

    @Override
    public PageResp<MaterialUsageResp> getMaterialUsagesByActivity(Integer userId, Integer farmingActivityId, Pageable pageable) {
        return searchAndFilterMaterialUsages(userId, farmingActivityId, null, null, null, null, null, pageable);
    }

    @Override
    public PageResp<MaterialUsageResp> searchAndFilterMaterialUsages(
            Integer userId,
            Integer farmingActivityId,
            Integer cropSeasonId,
            String keyword,
            String materialType,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable) {

        if (farmingActivityId == null && cropSeasonId == null) {
            throw new BadRequestException("Cần cung cấp farmingActivityId hoặc cropSeasonId để tra cứu sử dụng vật tư.");
        }

        if (farmingActivityId != null) {
            FarmingActivity activity = farmingActivityRepository.findById(farmingActivityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Hoạt động canh tác không tồn tại."));
            validateAndGetActiveMember(userId, activity);
        } else {
            CropSeason season = cropSeasonRepository.findById(cropSeasonId)
                    .orElseThrow(() -> new ResourceNotFoundException("Mùa vụ canh tác không tồn tại."));
            Farm farm = season.getFarm();
            farmMemberRepository.findByFarmIdAndUserIdAndStatus(farm.getId(), userId, "active")
                    .orElseThrow(() -> new ForbiddenException("Bạn không có quyền truy cập hoặc ghi nhật ký cho mùa vụ của trang trại này."));
        }

        Specification<MaterialUsage> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (farmingActivityId != null) {
                predicates.add(cb.equal(root.get("farmingActivity").get("id"), farmingActivityId));
            } else {
                predicates.add(cb.equal(root.get("farmingActivity").get("cropSeason").get("id"), cropSeasonId));
            }

            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("material").get("materialName")), pattern);
                Predicate notesMatch = cb.like(cb.lower(root.get("notes")), pattern);
                predicates.add(cb.or(nameMatch, notesMatch));
            }

            if (materialType != null && !materialType.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("material").get("materialType")), materialType.trim().toLowerCase()));
            }

            if (fromDate != null) {
                Instant fromInstant = fromDate.atZone(ZoneId.systemDefault()).toInstant();
                predicates.add(cb.greaterThanOrEqualTo(root.get("usedAt"), fromInstant));
            }

            if (toDate != null) {
                Instant toInstant = toDate.atZone(ZoneId.systemDefault()).toInstant();
                predicates.add(cb.lessThanOrEqualTo(root.get("usedAt"), toInstant));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<MaterialUsage> usagesPage = materialUsageRepository.findAll(spec, pageable);
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
