package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.cultivation.CreateHarvestRecordRequest;
import com.mycropdiary.api.dto.cultivation.HarvestRecordResponse;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import com.mycropdiary.api.entity.cultivation.HarvestRecord;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ForbiddenException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.cultivation.CropSeasonRepository;
import com.mycropdiary.api.repository.cultivation.HarvestRecordRepository;
import com.mycropdiary.api.service.cultivation.HarvestRecordService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Service implementation cho nghiệp vụ quản lý bản ghi thu hoạch (UC-26).
 */
@Service
@Transactional(readOnly = true)
public class HarvestRecordServiceImpl implements HarvestRecordService {

    private final HarvestRecordRepository harvestRecordRepository;
    private final CropSeasonRepository cropSeasonRepository;
    private final FarmMemberRepository farmMemberRepository;

    public HarvestRecordServiceImpl(
            HarvestRecordRepository harvestRecordRepository,
            CropSeasonRepository cropSeasonRepository,
            FarmMemberRepository farmMemberRepository) {
        this.harvestRecordRepository = harvestRecordRepository;
        this.cropSeasonRepository = cropSeasonRepository;
        this.farmMemberRepository = farmMemberRepository;
    }

    @Override
    @Transactional
    public HarvestRecordResponse createHarvestRecord(UUID userId, CreateHarvestRecordRequest request) {
        CropSeason season = cropSeasonRepository.findById(request.cropSeasonId())
                .orElseThrow(() -> new ResourceNotFoundException("Mùa vụ canh tác không tồn tại."));

        FarmMember recordedByMember = validateAndGetActiveMember(userId, season);

        String seasonStatus = season.getSeasonStatus();
        if (seasonStatus != null && ("completed".equalsIgnoreCase(seasonStatus)
                || "cancelled".equalsIgnoreCase(seasonStatus))) {
            throw new BadRequestException("Không thể ghi nhận thu hoạch cho mùa vụ đã đóng hoặc đã hủy.");
        }

        if (request.quantity() == null || request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Sản lượng thu hoạch phải lớn hơn 0.");
        }

        if (request.unit() == null || request.unit().trim().isEmpty()) {
            throw new BadRequestException("Đơn vị tính không được để trống.");
        }

        HarvestRecord record = new HarvestRecord();
        record.setFarm(season.getFarm());
        record.setCropSeason(season);
        record.setRecordedBy(recordedByMember);
        record.setHarvestDate(request.harvestedAt() != null ? request.harvestedAt().toLocalDate() : LocalDate.now());
        record.setQuantity(request.quantity());
        record.setUnit(request.unit().trim());
        record.setQualityGrade(request.qualityGrade() != null ? request.qualityGrade().trim() : null);
        record.setNotes(request.notes() != null ? request.notes().trim() : null);

        HarvestRecord savedRecord = harvestRecordRepository.save(record);
        return mapToResponse(savedRecord);
    }

    @Override
    public PageResponse<HarvestRecordResponse> getHarvestRecordsBySeason(UUID userId, UUID cropSeasonId, Pageable pageable) {
        CropSeason season = cropSeasonRepository.findById(cropSeasonId)
                .orElseThrow(() -> new ResourceNotFoundException("Mùa vụ canh tác không tồn tại."));

        validateAndGetActiveMember(userId, season);

        Page<HarvestRecord> page = harvestRecordRepository.findByCropSeasonId(cropSeasonId, pageable);
        return PageResponse.map(page, this::mapToResponse);
    }

    @Override
    public HarvestRecordResponse getHarvestRecordById(UUID userId, UUID harvestRecordId) {
        HarvestRecord record = harvestRecordRepository.findById(harvestRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Bản ghi thu hoạch không tồn tại."));

        validateAndGetActiveMember(userId, record.getCropSeason());

        return mapToResponse(record);
    }

    private FarmMember validateAndGetActiveMember(UUID userId, CropSeason season) {
        Farm farm = season.getFarm();
        return farmMemberRepository.findByFarmIdAndUserIdAndStatus(farm.getId(), userId, "active")
                .orElseThrow(() -> new ForbiddenException("Bạn không có quyền truy cập hoặc ghi nhận thu hoạch cho mùa vụ của trang trại này."));
    }

    private HarvestRecordResponse mapToResponse(HarvestRecord record) {
        CropSeason season = record.getCropSeason();
        Farm farm = season != null ? season.getFarm() : record.getFarm();
        Block block = record.getBlock();

        String recorderName = record.getRecordedBy() != null && record.getRecordedBy().getUser() != null
                ? record.getRecordedBy().getUser().getFullName()
                : null;

        return new HarvestRecordResponse(
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
