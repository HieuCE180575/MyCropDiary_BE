package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.CreateCropSeasonReq;
import com.mycropdiary.api.dto.cultivation.CropCategoryResp;
import com.mycropdiary.api.dto.cultivation.CropSeasonResp;
import com.mycropdiary.api.dto.cultivation.UpdateCropSeasonReq;
import com.mycropdiary.api.dto.cultivation.UpdateCropSeasonStatusReq;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.cultivation.CropCategory;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceConflictException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.repository.cultivation.CropCategoryRepository;
import com.mycropdiary.api.repository.cultivation.CropSeasonRepository;
import com.mycropdiary.api.repository.cultivation.FarmingActivityRepository;
import com.mycropdiary.api.repository.cultivation.HarvestRecordRepository;
import com.mycropdiary.api.service.cultivation.CropSeasonService;
import com.mycropdiary.api.util.FarmAccessGuard;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class CropSeasonServiceImpl implements CropSeasonService {

    private final CropSeasonRepository cropSeasonRepository;
    private final CropCategoryRepository cropCategoryRepository;
    private final FarmRepository farmRepository;
    private final UserRepository userRepository;
    private final FarmAccessGuard farmAccessGuard;
    private final FarmingActivityRepository farmingActivityRepository;
    private final HarvestRecordRepository harvestRecordRepository;

    public CropSeasonServiceImpl(CropSeasonRepository cropSeasonRepository,
                                 CropCategoryRepository cropCategoryRepository,
                                 FarmRepository farmRepository,
                                 UserRepository userRepository,
                                 FarmAccessGuard farmAccessGuard,
                                 FarmingActivityRepository farmingActivityRepository,
                                 HarvestRecordRepository harvestRecordRepository) {
        this.cropSeasonRepository = cropSeasonRepository;
        this.cropCategoryRepository = cropCategoryRepository;
        this.farmRepository = farmRepository;
        this.userRepository = userRepository;
        this.farmAccessGuard = farmAccessGuard;
        this.farmingActivityRepository = farmingActivityRepository;
        this.harvestRecordRepository = harvestRecordRepository;
    }

    @Override
    public CropSeasonResp createCropSeason(Integer userId, Integer farmId, CreateCropSeasonReq request) {
        farmAccessGuard.requireOwnerOrAdmin(userId, farmId);

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Trang trại không tồn tại ID: " + farmId));

        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("Ngày kết thúc vụ không thể trước ngày bắt đầu vụ.");
        }

        String trimmedName = request.seasonName().trim();
        if (cropSeasonRepository.existsByFarmIdAndSeasonNameIgnoreCase(farmId, trimmedName)) {
            throw new ResourceConflictException("Tên mùa vụ '" + trimmedName + "' đã tồn tại trong nông trại này.");
        }

        CropCategory category = cropCategoryRepository.findById(request.cropCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Loại cây trồng không tồn tại ID: " + request.cropCategoryId()));

        User creator = userRepository.findById(userId).orElse(null);

        String initialStatus;
        if (StringUtils.hasText(request.seasonStatus())) {
            initialStatus = request.seasonStatus().trim().toLowerCase();
        } else {
            initialStatus = !request.startDate().isAfter(LocalDate.now()) ? "active" : "planned";
        }

        CropSeason season = new CropSeason();
        season.setFarm(farm);
        season.setCropCategory(category);
        season.setSeasonName(trimmedName);
        season.setStartDate(request.startDate());
        season.setEndDate(request.endDate());
        season.setSeasonStatus(initialStatus);
        season.setNotes(request.notes() != null ? request.notes().trim() : null);
        season.setCreatedBy(creator);

        CropSeason saved = cropSeasonRepository.save(season);
        return mapToResponse(saved);
    }

    @Override
    public CropSeasonResp updateCropSeason(Integer userId, Integer cropSeasonId, UpdateCropSeasonReq request) {
        CropSeason season = cropSeasonRepository.findById(cropSeasonId)
                .orElseThrow(() -> new ResourceNotFoundException("Mùa vụ canh tác không tồn tại ID: " + cropSeasonId));

        Integer farmId = season.getFarm().getId();
        farmAccessGuard.requireOwnerOrAdmin(userId, farmId);

        String currentStatus = season.getSeasonStatus();
        if ("completed".equalsIgnoreCase(currentStatus) || "cancelled".equalsIgnoreCase(currentStatus)) {
            throw new BadRequestException("Không thể chỉnh sửa thông tin mùa vụ đã kết thúc hoặc đã hủy.");
        }

        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("Ngày kết thúc vụ không thể trước ngày bắt đầu vụ.");
        }

        String trimmedName = request.seasonName().trim();
        if (cropSeasonRepository.existsByFarmIdAndSeasonNameIgnoreCaseAndIdNot(farmId, trimmedName, cropSeasonId)) {
            throw new ResourceConflictException("Tên mùa vụ '" + trimmedName + "' đã tồn tại trong nông trại này.");
        }

        CropCategory category = cropCategoryRepository.findById(request.cropCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Loại cây trồng không tồn tại ID: " + request.cropCategoryId()));

        season.setSeasonName(trimmedName);
        season.setCropCategory(category);
        season.setStartDate(request.startDate());
        season.setEndDate(request.endDate());
        season.setNotes(request.notes() != null ? request.notes().trim() : null);

        CropSeason saved = cropSeasonRepository.save(season);
        return mapToResponse(saved);
    }

    @Override
    public CropSeasonResp updateCropSeasonStatus(Integer userId, Integer cropSeasonId, UpdateCropSeasonStatusReq request) {
        CropSeason season = cropSeasonRepository.findById(cropSeasonId)
                .orElseThrow(() -> new ResourceNotFoundException("Mùa vụ canh tác không tồn tại ID: " + cropSeasonId));

        farmAccessGuard.requireOwnerOrAdmin(userId, season.getFarm().getId());

        String newStatus = request.status().trim().toLowerCase();
        if ("completed".equalsIgnoreCase(newStatus) && season.getEndDate() == null) {
            season.setEndDate(LocalDate.now());
        }

        season.setSeasonStatus(newStatus);
        CropSeason saved = cropSeasonRepository.save(season);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CropSeasonResp getCropSeasonById(Integer userId, Integer cropSeasonId) {
        CropSeason season = cropSeasonRepository.findById(cropSeasonId)
                .orElseThrow(() -> new ResourceNotFoundException("Mùa vụ canh tác không tồn tại ID: " + cropSeasonId));

        farmAccessGuard.requireFarmMember(userId, season.getFarm().getId());

        return mapToResponse(season);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResp<CropSeasonResp> getCropSeasons(Integer userId, Integer farmId, String keyword, String status, Integer cropCategoryId, Pageable pageable) {
        farmAccessGuard.requireFarmMember(userId, farmId);

        Specification<CropSeason> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("farm").get("id"), farmId));

            if (StringUtils.hasText(status) && !"all".equalsIgnoreCase(status.trim())) {
                predicates.add(cb.equal(cb.lower(root.get("seasonStatus")), status.trim().toLowerCase()));
            }

            if (cropCategoryId != null) {
                predicates.add(cb.equal(root.get("cropCategory").get("id"), cropCategoryId));
            }

            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("seasonName")), pattern);
                Predicate notesLike = cb.like(cb.lower(root.get("notes")), pattern);
                predicates.add(cb.or(nameLike, notesLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<CropSeasonResp> page = cropSeasonRepository.findAll(spec, pageable)
                .map(this::mapToResponse);

        return PageResp.from(page);
    }

    @Override
    public void deleteCropSeason(Integer userId, Integer cropSeasonId) {
        CropSeason season = cropSeasonRepository.findById(cropSeasonId)
                .orElseThrow(() -> new ResourceNotFoundException("Mùa vụ canh tác không tồn tại ID: " + cropSeasonId));

        farmAccessGuard.requireOwnerOrAdmin(userId, season.getFarm().getId());

        boolean hasActivities = farmingActivityRepository.existsByCropSeasonId(cropSeasonId);
        boolean hasHarvests = harvestRecordRepository.existsByCropSeasonId(cropSeasonId);

        if (hasActivities || hasHarvests) {
            throw new BadRequestException("Không thể xóa mùa vụ vì đã có nhật ký canh tác hoặc thu hoạch liên kết. Bạn có thể chuyển trạng thái sang đã hủy (cancelled).");
        }

        cropSeasonRepository.delete(season);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CropCategoryResp> getCropCategories() {
        return cropCategoryRepository.findAll().stream()
                .map(c -> new CropCategoryResp(c.getId(), c.getCategoryName(), c.getDescription(), c.getImageUrl()))
                .toList();
    }

    private CropSeasonResp mapToResponse(CropSeason cs) {
        String farmName = cs.getFarm() != null ? cs.getFarm().getFarmName() : null;
        Integer cropCatId = cs.getCropCategory() != null ? cs.getCropCategory().getId() : null;
        String cropCatName = cs.getCropCategory() != null ? cs.getCropCategory().getCategoryName() : null;
        Integer creatorId = cs.getCreatedBy() != null ? cs.getCreatedBy().getId() : null;
        String creatorName = cs.getCreatedBy() != null ? cs.getCreatedBy().getFullName() : null;

        return new CropSeasonResp(
                cs.getId(),
                cs.getFarm() != null ? cs.getFarm().getId() : null,
                farmName,
                cropCatId,
                cropCatName,
                cs.getSeasonName(),
                cs.getStartDate(),
                cs.getEndDate(),
                cs.getSeasonStatus(),
                cs.getNotes(),
                creatorId,
                creatorName,
                cs.getCreatedAt()
        );
    }

    private CropCategory resolveCategory(Integer categoryId, String categoryName) {
        if (categoryId != null && categoryId > 0) {
            CropCategory found = cropCategoryRepository.findById(categoryId).orElse(null);
            if (found != null) return found;
        }
        if (StringUtils.hasText(categoryName)) {
            String trimmed = categoryName.trim();
            return cropCategoryRepository.findByCategoryNameIgnoreCase(trimmed)
                    .orElseGet(() -> {
                        CropCategory newCat = new CropCategory(trimmed, "Cây ngắn ngày canh tác theo quy trình VietGAP");
                        return cropCategoryRepository.save(newCat);
                    });
        }
        if (categoryId != null) {
            throw new ResourceNotFoundException("Loại cây trồng không tồn tại ID: " + categoryId);
        }
        throw new BadRequestException("Vui lòng chọn hoặc nhập tên loại cây trồng hợp lệ.");
    }

}
