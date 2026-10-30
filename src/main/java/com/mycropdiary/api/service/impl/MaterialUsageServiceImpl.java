package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.*;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.cultivation.*;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ForbiddenException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.cultivation.*;
import com.mycropdiary.api.service.cultivation.MaterialUsageService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Optional;

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
    private final FarmRepository farmRepository;

    public MaterialUsageServiceImpl(
            MaterialUsageRepository materialUsageRepository,
            MaterialRepository materialRepository,
            FarmingActivityRepository farmingActivityRepository,
            FarmMemberRepository farmMemberRepository,
            CropSeasonRepository cropSeasonRepository) {
        this(materialUsageRepository, materialRepository, farmingActivityRepository, farmMemberRepository, cropSeasonRepository, null);
    }

    @Autowired
    public MaterialUsageServiceImpl(
            MaterialUsageRepository materialUsageRepository,
            MaterialRepository materialRepository,
            FarmingActivityRepository farmingActivityRepository,
            FarmMemberRepository farmMemberRepository,
            CropSeasonRepository cropSeasonRepository,
            @Autowired(required = false) FarmRepository farmRepository) {
        this.materialUsageRepository = materialUsageRepository;
        this.materialRepository = materialRepository;
        this.farmingActivityRepository = farmingActivityRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.cropSeasonRepository = cropSeasonRepository;
        this.farmRepository = farmRepository;
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
        if (material.getUnit() != null && !isCompatibleUnit(material.getUnit(), request.unit().trim())) {
            throw new BadRequestException("Đơn vị tính (" + request.unit().trim() + ") không khớp với đơn vị tính của vật tư (" + material.getUnit() + ").");
        }

        // 9. Validate Pesticide Safety Interval & Applied Area / Plant Count
        if (request.safetyIntervalDays() != null && request.safetyIntervalDays() < 0) {
            throw new BadRequestException("Thời gian cách ly phải >= 0.");
        }
        if (request.appliedArea() != null && request.appliedArea().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Diện tích áp dụng phải lớn hơn 0.");
        }
        if (request.appliedPlantCount() != null && request.appliedPlantCount() <= 0) {
            throw new BadRequestException("Số cây áp dụng phải lớn hơn 0.");
        }

        String resolvedMaterialForm = cleanText(request.materialForm());
        if (resolvedMaterialForm == null) {
            resolvedMaterialForm = cleanText(material.getMaterialForm());
        }
        if (resolvedMaterialForm == null) {
            resolvedMaterialForm = inferMaterialForm(material.getMaterialType(), material.getUnit());
        }
        if (material.getMaterialForm() == null && resolvedMaterialForm != null) {
            material.setMaterialForm(resolvedMaterialForm);
            materialRepository.save(material);
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
        usage.setDosage(cleanText(request.dosage()));
        usage.setMethod(cleanText(request.method()));
        usage.setMaterialForm(resolvedMaterialForm);
        usage.setAppliedArea(request.appliedArea());
        usage.setAppliedAreaUnit(request.appliedArea() != null
                ? (cleanText(request.appliedAreaUnit()) != null ? cleanText(request.appliedAreaUnit()) : "m²")
                : null);
        usage.setAppliedPlantCount(request.appliedPlantCount());
        usage.setSafetyIntervalDays(request.safetyIntervalDays());

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
                    && !isCompatibleUnit(currentMaterial.getUnit(), request.unit().trim())) {
                throw new BadRequestException("Đơn vị tính (" + request.unit().trim() + ") không khớp với đơn vị tính của vật tư (" + currentMaterial.getUnit() + ").");
            }
            usage.setUnit(request.unit().trim());
        }

        // 6. Nếu cập nhật thời gian sử dụng (usedAt)
        if (request.usedAt() != null) {
            usage.setUsedAt(request.usedAt());
        }

        // 7. Validate thời gian cách ly (safetyIntervalDays) & diện tích / số cây
        if (request.safetyIntervalDays() != null) {
            if (request.safetyIntervalDays() < 0) {
                throw new BadRequestException("Thời gian cách ly phải >= 0.");
            }
            usage.setSafetyIntervalDays(request.safetyIntervalDays());
        }
        if (request.appliedArea() != null && request.appliedArea().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Diện tích áp dụng phải lớn hơn 0.");
        }
        if (request.appliedPlantCount() != null && request.appliedPlantCount() <= 0) {
            throw new BadRequestException("Số cây áp dụng phải lớn hơn 0.");
        }

        // 8. Cập nhật các trường bổ sung
        if (request.dosage() != null) {
            usage.setDosage(cleanText(request.dosage()));
        }
        if (request.method() != null) {
            usage.setMethod(cleanText(request.method()));
        }
        if (request.materialForm() != null) {
            String form = cleanText(request.materialForm());
            usage.setMaterialForm(form != null ? form : (currentMaterial != null ? currentMaterial.getMaterialForm() : null));
        }
        usage.setAppliedArea(request.appliedArea());
        usage.setAppliedAreaUnit(request.appliedArea() != null
                ? (cleanText(request.appliedAreaUnit()) != null ? cleanText(request.appliedAreaUnit()) : "m²")
                : null);
        usage.setAppliedPlantCount(request.appliedPlantCount());

        if (request.notes() != null) {
            usage.setNotes(request.notes().trim());
        }

        MaterialUsage savedUsage = materialUsageRepository.save(usage);
        return mapToResponse(savedUsage);
    }

    @Override
    public PageResp<MaterialUsageResp> getMaterialUsagesByActivity(Integer userId, Integer farmingActivityId, Pageable pageable) {
        return searchAndFilterMaterialUsages(userId, null, farmingActivityId, null, null, null, null, null, pageable);
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
        return searchAndFilterMaterialUsages(userId, null, farmingActivityId, cropSeasonId, keyword, materialType, fromDate, toDate, pageable);
    }

    @Override
    public PageResp<MaterialUsageResp> searchAndFilterMaterialUsages(
            Integer userId,
            Integer farmId,
            Integer farmingActivityId,
            Integer cropSeasonId,
            String keyword,
            String materialType,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable) {

        if (farmingActivityId == null && cropSeasonId == null && farmId == null) {
            throw new BadRequestException("Cần cung cấp farmingActivityId hoặc cropSeasonId để tra cứu sử dụng vật tư.");
        }

        if (farmingActivityId != null) {
            FarmingActivity activity = farmingActivityRepository.findById(farmingActivityId)
                    .orElseThrow(() -> new ResourceNotFoundException("Hoạt động canh tác không tồn tại."));
            validateAndGetActiveMember(userId, activity);
        } else if (cropSeasonId != null) {
            CropSeason season = cropSeasonRepository.findById(cropSeasonId)
                    .orElseThrow(() -> new ResourceNotFoundException("Mùa vụ canh tác không tồn tại."));
            Farm farm = season.getFarm();
            farmMemberRepository.findByFarmIdAndUserIdAndStatus(farm.getId(), userId, "active")
                    .orElseThrow(() -> new ForbiddenException("Bạn không có quyền truy cập hoặc ghi nhật ký cho mùa vụ của trang trại này."));
        } else {
            farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, userId, "active")
                    .orElseThrow(() -> new ForbiddenException("Bạn không có quyền truy cập hoặc ghi nhật ký cho trang trại này."));
        }

        Specification<MaterialUsage> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (farmingActivityId != null) {
                predicates.add(cb.equal(root.get("farmingActivity").get("id"), farmingActivityId));
            } else if (cropSeasonId != null) {
                predicates.add(cb.equal(root.get("farmingActivity").get("cropSeason").get("id"), cropSeasonId));
            } else if (farmId != null) {
                predicates.add(cb.equal(root.get("farm").get("id"), farmId));
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
    @Transactional
    public void deleteMaterialUsage(Integer userId, Integer materialUsageId) {
        MaterialUsage usage = materialUsageRepository.findById(materialUsageId)
                .orElseThrow(() -> new ResourceNotFoundException("Bản ghi sử dụng vật tư không tồn tại."));

        FarmingActivity activity = usage.getFarmingActivity();
        if (activity != null) {
            validateAndGetActiveMember(userId, activity);
            CropSeason season = activity.getCropSeason();
            if (season != null) {
                String seasonStatus = season.getSeasonStatus();
                if (seasonStatus != null && ("completed".equalsIgnoreCase(seasonStatus)
                        || "cancelled".equalsIgnoreCase(seasonStatus)
                        || "archived".equalsIgnoreCase(seasonStatus))) {
                    throw new BadRequestException("Không thể xóa sử dụng vật tư cho mùa vụ đã đóng hoặc đã hủy.");
                }
            }
        } else if (usage.getFarm() != null) {
            farmMemberRepository.findByFarmIdAndUserIdAndStatus(usage.getFarm().getId(), userId, "active")
                    .orElseThrow(() -> new ForbiddenException("Bạn không có quyền thao tác trên trang trại này."));
        }

        materialUsageRepository.delete(usage);
    }

    @Override
    @Transactional
    public List<MaterialOptionResp> getMaterialsByFarm(Integer userId, Integer farmId) {
        farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, userId, "active")
                .orElseThrow(() -> new ForbiddenException("Bạn không có quyền truy cập trang trại này."));

        List<Material> materials = materialRepository.findAllByFarmIdAndIsActiveTrue(farmId);
        if (materials.isEmpty() && farmRepository != null) {
            Optional<Farm> farmOpt = farmRepository.findById(farmId);
            if (farmOpt.isPresent() && materialRepository.findAllByFarmId(farmId).isEmpty()) {
                Farm farm = farmOpt.get();
                List<Material> defaults = List.of(
                        createDefaultMaterial(farm, "Phân trùn quế Vermicompost", "Phân bón hữu cơ", "Dạng hữu cơ vi sinh", "bao", "Phân hữu cơ vi sinh giàu mùn cải tạo đất chuẩn VietGAP (Bao 50kg)"),
                        createDefaultMaterial(farm, "Phân NPK 20-20-20 Haifa", "Phân bón hòa tan", "Dạng bột", "bao", "Phân bón NPK tinh khiết tan hoàn toàn dùng cho hệ thống tưới (Bao 25kg)"),
                        createDefaultMaterial(farm, "Dung dịch thủy canh A+B GreenHydro", "Dinh dưỡng lỏng", "Dạng lỏng / dung dịch", "can", "Dinh dưỡng khoáng đa trung vi lượng đậm đặc (Can 5L)"),
                        createDefaultMaterial(farm, "Chế phẩm sinh học trừ sâu Neem Chito", "Thuốc BVTV sinh học", "Dạng nhũ dầu", "chai", "Dầu neem kết hợp chitosan phòng trừ bọ trĩ và nấm bệnh (Chai 1L)"),
                        createDefaultMaterial(farm, "Chế phẩm nấm đối kháng Trichoderma", "Chế phẩm sinh học", "Dạng bột", "gói", "Chế phẩm vi sinh ủ phân và phòng nấm bệnh rễ"),
                        createDefaultMaterial(farm, "Hạt giống dưa lưới F1 Taka Nhật Bản", "Hạt giống", "Hạt / cây giống", "gói", "Hạt giống tỷ lệ nảy mầm cao đạt chuẩn VietGAP")
                );
                materials = materialRepository.saveAll(defaults);
            }
        }
        for (Material m : materials) {
            if (m.getMaterialForm() == null || m.getMaterialForm().isBlank()) {
                m.setMaterialForm(inferMaterialForm(m.getMaterialType(), m.getUnit()));
            }
        }
        return materials.stream().map(MaterialOptionResp::fromEntity).toList();
    }

    private Material createDefaultMaterial(Farm farm, String name, String type, String form, String unit, String description) {
        Material m = new Material(farm, name, type, form, unit);
        m.setDescription(description);
        return m;
    }

    @Override
    @Transactional
    public MaterialOptionResp createMaterial(Integer userId, CreateMaterialReq request) {
        farmMemberRepository.findByFarmIdAndUserIdAndStatus(request.farmId(), userId, "active")
                .orElseThrow(() -> new ForbiddenException("Bạn không có quyền quản lý vật tư của trang trại này."));

        String trimmedName = request.materialName().trim();
        String trimmedType = cleanText(request.materialType());
        String trimmedUnit = request.unit().trim();
        String resolvedForm = cleanText(request.materialForm());
        if (resolvedForm == null) {
            resolvedForm = inferMaterialForm(trimmedType, trimmedUnit);
        }

        Optional<Material> existingOpt = materialRepository.findByFarmIdAndMaterialNameIgnoreCase(request.farmId(), trimmedName);
        if (existingOpt.isPresent()) {
            Material existing = existingOpt.get();
            existing.setIsActive(true);
            if (trimmedType != null) {
                existing.setMaterialType(trimmedType);
            }
            if (resolvedForm != null) {
                existing.setMaterialForm(resolvedForm);
            }
            existing.setUnit(trimmedUnit);
            if (request.description() != null && !request.description().trim().isEmpty()) {
                existing.setDescription(request.description().trim());
            }
            return MaterialOptionResp.fromEntity(materialRepository.save(existing));
        }

        Farm farm;
        if (farmRepository != null) {
            farm = farmRepository.findById(request.farmId())
                    .orElseThrow(() -> new ResourceNotFoundException("Trang trại không tồn tại."));
        } else {
            farm = new Farm();
            farm.setId(request.farmId());
        }

        Material material = new Material(
                farm,
                trimmedName,
                trimmedType,
                resolvedForm,
                trimmedUnit
        );
        if (request.description() != null) {
            material.setDescription(request.description().trim());
        }
        Material saved = materialRepository.save(material);
        return MaterialOptionResp.fromEntity(saved);
    }

    @Override
    public MaterialUsageResp getMaterialUsageById(Integer userId, Integer materialUsageId) {
        MaterialUsage usage = materialUsageRepository.findById(materialUsageId)
                .orElseThrow(() -> new ResourceNotFoundException("Bản ghi sử dụng vật tư không tồn tại."));

        if (usage.getFarmingActivity() != null) {
            validateAndGetActiveMember(userId, usage.getFarmingActivity());
        } else if (usage.getFarm() != null) {
            farmMemberRepository.findByFarmIdAndUserIdAndStatus(usage.getFarm().getId(), userId, "active")
                    .orElseThrow(() -> new ForbiddenException("Bạn không có quyền truy cập hoặc ghi nhật ký cho mùa vụ của trang trại này."));
        }

        return mapToResponse(usage);
    }

    private FarmMember validateAndGetActiveMember(Integer userId, FarmingActivity activity) {
        CropSeason season = activity.getCropSeason();
        Farm farm = season != null ? season.getFarm() : activity.getFarm();

        return farmMemberRepository.findByFarmIdAndUserIdAndStatus(farm.getId(), userId, "active")
                .orElseThrow(() -> new ForbiddenException("Bạn không có quyền truy cập hoặc ghi nhật ký cho mùa vụ của trang trại này."));
    }

    private boolean isCompatibleUnit(String materialUnit, String requestUnit) {
        if (materialUnit == null || requestUnit == null) return true;
        String a = materialUnit.trim().toLowerCase();
        String b = requestUnit.trim().toLowerCase();
        if (a.equals(b)) return true;

        // Liter / Liquid / Bottle (chai) / Can / Thùng compatibility
        if (isLiquidPackagingUnit(a) && isLiquidPackagingUnit(b)) return true;

        // Solid / Fertilizer / Bag (bao) / Ton (tấn) / Kg compatibility
        if (isSolidPackagingUnit(a) && isSolidPackagingUnit(b)) return true;

        return false;
    }

    private boolean isLiquidPackagingUnit(String u) {
        if (u == null) return false;
        String s = u.toLowerCase();
        return s.equals("l") || s.equals("lít") || s.equals("lit") || s.equals("ml")
                || s.contains("chai") || s.contains("can") || s.contains("thùng") || s.contains("lọ");
    }

    private boolean isSolidPackagingUnit(String u) {
        if (u == null) return false;
        String s = u.toLowerCase();
        return s.equals("kg") || s.equals("kilogram") || s.equals("g") || s.equals("gram")
                || s.contains("bao") || s.equals("tấn") || s.equals("tan") || s.equals("tạ") || s.equals("ta")
                || s.contains("gói") || s.contains("thùng");
    }

    private String cleanText(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String inferMaterialForm(String materialType, String unit) {
        String type = materialType != null ? materialType.toLowerCase() : "";
        String u = unit != null ? unit.toLowerCase() : "";
        if (type.contains("lỏng") || u.equals("l") || u.equals("lít") || u.equals("ml") || u.equals("can")) {
            return "Dạng lỏng / dung dịch";
        }
        if (type.contains("bvtv") || u.equals("chai")) {
            return "Dạng nhũ dầu";
        }
        if (type.contains("hòa tan")) {
            return "Dạng bột";
        }
        if (type.contains("hữu cơ") || type.contains("sinh học")) {
            return "Dạng hữu cơ vi sinh";
        }
        if (type.contains("giống") || u.equals("hạt") || u.equals("cây") || u.equals("khay")) {
            return "Hạt / cây giống";
        }
        if (u.equals("viên")) {
            return "Dạng viên nén";
        }
        return "Dạng hạt";
    }

    private MaterialUsageResp mapToResponse(MaterialUsage usage) {
        String recorderName = usage.getUsedBy() != null && usage.getUsedBy().getUser() != null
                ? usage.getUsedBy().getUser().getFullName()
                : null;
        FarmingActivity act = usage.getFarmingActivity();
        CropSeason cs = act != null ? act.getCropSeason() : null;
        Material mat = usage.getMaterial();
        String materialForm = cleanText(usage.getMaterialForm());
        if (materialForm == null && mat != null) {
            materialForm = cleanText(mat.getMaterialForm());
            if (materialForm == null) {
                materialForm = inferMaterialForm(mat.getMaterialType(), mat.getUnit());
            }
        }

        return new MaterialUsageResp(
                usage.getId(),
                act != null ? act.getId() : null,
                act != null ? act.getActivityName() : null,
                cs != null ? cs.getId() : null,
                mat != null ? mat.getId() : null,
                mat != null ? mat.getMaterialName() : null,
                mat != null ? mat.getMaterialName() : null,
                mat != null ? mat.getMaterialType() : null,
                materialForm,
                null,
                usage.getUsedBy() != null ? usage.getUsedBy().getId() : null,
                recorderName,
                usage.getUsedAtLdt(),
                usage.getQuantity(),
                usage.getUnit(),
                usage.getDosage(),
                usage.getMethod(),
                usage.getAppliedArea(),
                usage.getAppliedAreaUnit(),
                usage.getAppliedPlantCount(),
                usage.getSafetyIntervalDays(),
                usage.getNotes()
        );
    }
}
