package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.CreateHarvestRecordReq;
import com.mycropdiary.api.dto.cultivation.HarvestRecordResp;
import com.mycropdiary.api.dto.cultivation.UpdateHarvestRecordReq;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import com.mycropdiary.api.entity.cultivation.HarvestRecord;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ForbiddenException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.BlockRepository;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.cultivation.CropSeasonBlockRepository;
import com.mycropdiary.api.repository.cultivation.CropSeasonRepository;
import com.mycropdiary.api.repository.cultivation.HarvestRecordRepository;
import com.mycropdiary.api.service.cultivation.HarvestRecordService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service implementation cho nghiệp vụ quản lý bản ghi thu hoạch (UC-26).
 */
@Service
@Transactional(readOnly = true)
public class HarvestRecordServiceImpl implements HarvestRecordService {

    private final HarvestRecordRepository harvestRecordRepository;
    private final CropSeasonRepository cropSeasonRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final BlockRepository blockRepository;
    private final CropSeasonBlockRepository cropSeasonBlockRepository;

    @Autowired
    public HarvestRecordServiceImpl(
            HarvestRecordRepository harvestRecordRepository,
            CropSeasonRepository cropSeasonRepository,
            FarmMemberRepository farmMemberRepository,
            BlockRepository blockRepository,
            CropSeasonBlockRepository cropSeasonBlockRepository) {
        this.harvestRecordRepository = harvestRecordRepository;
        this.cropSeasonRepository = cropSeasonRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.blockRepository = blockRepository;
        this.cropSeasonBlockRepository = cropSeasonBlockRepository;
    }

    public HarvestRecordServiceImpl(
            HarvestRecordRepository harvestRecordRepository,
            CropSeasonRepository cropSeasonRepository,
            FarmMemberRepository farmMemberRepository,
            BlockRepository blockRepository) {
        this(harvestRecordRepository, cropSeasonRepository, farmMemberRepository, blockRepository, null);
    }

    public HarvestRecordServiceImpl(
            HarvestRecordRepository harvestRecordRepository,
            CropSeasonRepository cropSeasonRepository,
            FarmMemberRepository farmMemberRepository) {
        this(harvestRecordRepository, cropSeasonRepository, farmMemberRepository, null, null);
    }

    @Override
    @Transactional
    public HarvestRecordResp createHarvestRecord(Integer userId, CreateHarvestRecordReq request) {
        CropSeason season = cropSeasonRepository.findById(request.cropSeasonId())
                .orElseThrow(() -> new ResourceNotFoundException("Mùa vụ canh tác không tồn tại."));

        FarmMember recordedByMember = validateAndGetActiveMember(userId, season);

        String seasonStatus = season.getSeasonStatus();
        if (seasonStatus != null && ("completed".equalsIgnoreCase(seasonStatus)
                || "cancelled".equalsIgnoreCase(seasonStatus)
                || "archived".equalsIgnoreCase(seasonStatus))) {
            throw new BadRequestException("Không thể ghi nhận thu hoạch cho mùa vụ đã đóng hoặc đã hủy.");
        }

        if (request.quantity() == null || request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Sản lượng thu hoạch phải lớn hơn 0.");
        }

        if (request.unit() == null || request.unit().trim().isEmpty()) {
            throw new BadRequestException("Đơn vị tính không được để trống.");
        }

        LocalDate harvestDate = request.harvestedAt() != null ? request.harvestedAt().toLocalDate() : LocalDate.now();
        if (season.getStartDate() != null && harvestDate.isBefore(season.getStartDate())) {
            throw new BadRequestException("Ngày thu hoạch (" + harvestDate + ") không thể trước ngày bắt đầu mùa vụ (" + season.getStartDate() + ").");
        }

        if (request.blockId() != null && cropSeasonBlockRepository != null) {
            cropSeasonBlockRepository.findByCropSeasonIdAndBlockIdWithDetails(season.getId(), request.blockId())
                    .ifPresent(csb -> {
                        if (csb.getPlantedAt() != null && harvestDate.isBefore(csb.getPlantedAt())) {
                            throw new BadRequestException("Ngày thu hoạch (" + harvestDate + ") không thể trước ngày xuống giống tại lô đất (" + csb.getPlantedAt() + ").");
                        }
                    });
        }

        HarvestRecord record = new HarvestRecord();
        record.setFarm(season.getFarm());
        record.setCropSeason(season);
        record.setRecordedBy(recordedByMember);
        record.setHarvestDate(harvestDate);
        record.setQuantity(request.quantity());
        record.setUnit(request.unit().trim());
        record.setQualityGrade(request.qualityGrade() != null ? request.qualityGrade().trim() : null);
        record.setNotes(request.notes() != null ? request.notes().trim() : null);

        if (request.blockId() != null && blockRepository != null) {
            blockRepository.findById(request.blockId()).ifPresent(record::setBlock);
        }

        HarvestRecord savedRecord = harvestRecordRepository.save(record);
        return mapToResponse(savedRecord);
    }

    @Override
    @Transactional
    public HarvestRecordResp updateHarvestRecord(Integer userId, Integer harvestRecordId, UpdateHarvestRecordReq request) {
        HarvestRecord record = harvestRecordRepository.findById(harvestRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Bản ghi thu hoạch không tồn tại."));

        CropSeason season = record.getCropSeason();
        validateAndGetActiveMember(userId, season);

        if (season != null) {
            String seasonStatus = season.getSeasonStatus();
            if (seasonStatus != null && ("completed".equalsIgnoreCase(seasonStatus)
                    || "cancelled".equalsIgnoreCase(seasonStatus)
                    || "archived".equalsIgnoreCase(seasonStatus))) {
                throw new BadRequestException("Không thể chỉnh sửa bản ghi thu hoạch cho mùa vụ đã đóng hoặc đã hủy.");
            }
        }

        if (request.quantity() != null) {
            if (request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Sản lượng thu hoạch phải lớn hơn 0.");
            }
            record.setQuantity(request.quantity());
        }

        if (request.unit() != null && !request.unit().trim().isEmpty()) {
            record.setUnit(request.unit().trim());
        }

        LocalDate nextHarvestDate = request.harvestedAt() != null ? request.harvestedAt().toLocalDate() : record.getHarvestDate();
        if (season != null && season.getStartDate() != null && nextHarvestDate != null && nextHarvestDate.isBefore(season.getStartDate())) {
            throw new BadRequestException("Ngày thu hoạch (" + nextHarvestDate + ") không thể trước ngày bắt đầu mùa vụ (" + season.getStartDate() + ").");
        }

        Integer nextBlockId = request.blockId() != null ? request.blockId() : (record.getBlock() != null ? record.getBlock().getId() : null);
        if (season != null && nextBlockId != null && cropSeasonBlockRepository != null && nextHarvestDate != null) {
            cropSeasonBlockRepository.findByCropSeasonIdAndBlockIdWithDetails(season.getId(), nextBlockId)
                    .ifPresent(csb -> {
                        if (csb.getPlantedAt() != null && nextHarvestDate.isBefore(csb.getPlantedAt())) {
                            throw new BadRequestException("Ngày thu hoạch (" + nextHarvestDate + ") không thể trước ngày xuống giống tại lô đất (" + csb.getPlantedAt() + ").");
                        }
                    });
        }

        if (request.harvestedAt() != null) {
            record.setHarvestDate(nextHarvestDate);
        }

        if (request.qualityGrade() != null) {
            record.setQualityGrade(request.qualityGrade().trim());
        }

        if (request.notes() != null) {
            record.setNotes(request.notes().trim());
        }

        if (request.blockId() != null && blockRepository != null) {
            Block block = blockRepository.findById(request.blockId())
                    .orElseThrow(() -> new ResourceNotFoundException("Khu đất canh tác không tồn tại."));
            record.setBlock(block);
        }

        HarvestRecord savedRecord = harvestRecordRepository.save(record);
        return mapToResponse(savedRecord);
    }

    @Override
    public PageResp<HarvestRecordResp> getHarvestRecordsBySeason(Integer userId, Integer cropSeasonId, Pageable pageable) {
        return searchAndFilterHarvestRecords(userId, cropSeasonId, null, null, null, null, pageable);
    }

    @Override
    public PageResp<HarvestRecordResp> searchAndFilterHarvestRecords(
            Integer userId,
            Integer cropSeasonId,
            String keyword,
            String qualityGrade,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable) {

        CropSeason season = cropSeasonRepository.findById(cropSeasonId)
                .orElseThrow(() -> new ResourceNotFoundException("Mùa vụ canh tác không tồn tại."));

        validateAndGetActiveMember(userId, season);

        Specification<HarvestRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("cropSeason").get("id"), cropSeasonId));

            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate notesMatch = cb.like(cb.lower(root.get("notes")), pattern);
                Predicate qualityMatch = cb.like(cb.lower(root.get("qualityGrade")), pattern);
                predicates.add(cb.or(notesMatch, qualityMatch));
            }

            if (qualityGrade != null && !qualityGrade.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("qualityGrade")), qualityGrade.trim().toLowerCase()));
            }

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("harvestDate"), fromDate.toLocalDate()));
            }

            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("harvestDate"), toDate.toLocalDate()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<HarvestRecord> page = harvestRecordRepository.findAll(spec, pageable);
        return PageResp.map(page, this::mapToResponse);
    }

    @Override
    public HarvestRecordResp getHarvestRecordById(Integer userId, Integer harvestRecordId) {
        HarvestRecord record = harvestRecordRepository.findById(harvestRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Bản ghi thu hoạch không tồn tại."));

        validateAndGetActiveMember(userId, record.getCropSeason());

        return mapToResponse(record);
    }

    private FarmMember validateAndGetActiveMember(Integer userId, CropSeason season) {
        Farm farm = season.getFarm();
        return farmMemberRepository.findByFarmIdAndUserIdAndStatus(farm.getId(), userId, "active")
                .orElseThrow(() -> new ForbiddenException("Bạn không có quyền truy cập hoặc ghi nhận thu hoạch cho mùa vụ của trang trại này."));
    }

    private HarvestRecordResp mapToResponse(HarvestRecord record) {
        CropSeason season = record.getCropSeason();
        Farm farm = season != null ? season.getFarm() : record.getFarm();
        Block block = record.getBlock();

        String recorderName = record.getRecordedBy() != null && record.getRecordedBy().getUser() != null
                ? record.getRecordedBy().getUser().getFullName()
                : null;

        return new HarvestRecordResp(
                record.getId(),
                season != null ? season.getId() : null,
                season != null ? season.getSeasonName() : null,
                season != null ? season.getSeasonName() : null,
                farm != null ? farm.getId() : null,
                farm != null ? farm.getFarmName() : null,
                block != null ? block.getId() : null,
                block != null ? block.getBlockName() : null,
                block != null ? block.getBlockName() : null,
                record.getRecordedBy() != null ? record.getRecordedBy().getId() : null,
                recorderName,
                record.getHarvestLotCode(),
                record.getHarvestedAt(),
                record.getQuantity(),
                record.getUnit(),
                record.getQualityGrade(),
                record.getStorageLocation(),
                record.getTraceabilityCode(),
                record.getNotes()
        );
    }
}
