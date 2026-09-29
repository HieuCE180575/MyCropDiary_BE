package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.cultivation.CreateHarvestRecordRequest;
import com.mycropdiary.api.dto.cultivation.HarvestRecordResponse;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.ProductionArea;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import com.mycropdiary.api.entity.cultivation.HarvestRecord;
import com.mycropdiary.api.entity.cultivation.Plot;

import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ForbiddenException;
import com.mycropdiary.api.exception.ResourceNotFoundException;

import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.StaffAreaAssignmentRepository;
import com.mycropdiary.api.repository.cultivation.CropSeasonRepository;
import com.mycropdiary.api.repository.cultivation.HarvestRecordRepository;

import com.mycropdiary.api.service.cultivation.HarvestRecordService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final StaffAreaAssignmentRepository staffAreaAssignmentRepository;

    public HarvestRecordServiceImpl(
            HarvestRecordRepository harvestRecordRepository,
            CropSeasonRepository cropSeasonRepository,
            FarmMemberRepository farmMemberRepository,
            StaffAreaAssignmentRepository staffAreaAssignmentRepository) {
        this.harvestRecordRepository = harvestRecordRepository;
        this.cropSeasonRepository = cropSeasonRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.staffAreaAssignmentRepository = staffAreaAssignmentRepository;
    }

    @Override
    @Transactional
    public HarvestRecordResponse createHarvestRecord(Long userId, CreateHarvestRecordRequest request) {
        // 1. Tìm CropSeason
        CropSeason season = cropSeasonRepository.findById(request.cropSeasonId())
                .orElseThrow(() -> new ResourceNotFoundException("Mùa vụ canh tác không tồn tại."));

        // 2. Validate Authorization & lấy active FarmMember
        FarmMember recordedByMember = validateAndGetActiveMember(userId, season);

        // 3. Kiểm tra trạng thái mùa vụ (Chỉ giữ COMPLETED/CANCELLED không được tạo bản ghi thu hoạch)
        String seasonStatus = season.getStatus();
        if (seasonStatus != null && ("COMPLETED".equalsIgnoreCase(seasonStatus)
                || "CANCELLED".equalsIgnoreCase(seasonStatus))) {
            throw new BadRequestException("Không thể ghi nhận thu hoạch cho mùa vụ đã đóng hoặc đã hủy.");
        }

        // 4. Validate Quantity > 0 (Theo SQL constraint Quantity > 0)
        if (request.quantity() == null || request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Sản lượng thu hoạch phải lớn hơn 0.");
        }

        // 5. Validate Unit
        if (request.unit() == null || request.unit().trim().isEmpty()) {
            throw new BadRequestException("Đơn vị tính không được để trống.");
        }

        // 6. Xử lý HarvestLotCode (Truyền lên hoặc tự sinh kèm UUID + Timestamp để đảm bảo tính duy nhất)
        String lotCode;
        if (request.harvestLotCode() != null && !request.harvestLotCode().trim().isEmpty()) {
            lotCode = request.harvestLotCode().trim();
            if (harvestRecordRepository.existsByHarvestLotCode(lotCode)) {
                throw new BadRequestException("Mã lô thu hoạch (HarvestLotCode) đã tồn tại: " + lotCode);
            }
        } else {
            lotCode = "HRV-S" + season.getId() + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        // 7. Validate TraceabilityCode nếu có
        String traceabilityCode = null;
        if (request.traceabilityCode() != null && !request.traceabilityCode().trim().isEmpty()) {
            traceabilityCode = request.traceabilityCode().trim();
            if (harvestRecordRepository.existsByTraceabilityCode(traceabilityCode)) {
                throw new BadRequestException("Mã truy xuất nguồn gốc (TraceabilityCode) đã tồn tại: " + traceabilityCode);
            }
        }

        // 8. Khởi tạo HarvestRecord
        HarvestRecord record = new HarvestRecord();
        record.setCropSeason(season);
        record.setRecordedByMember(recordedByMember);
        record.setHarvestLotCode(lotCode);
        record.setHarvestedAt(request.harvestedAt());
        record.setQuantity(request.quantity());
        record.setUnit(request.unit().trim());
        record.setQualityGrade(request.qualityGrade() != null ? request.qualityGrade().trim() : null);
        record.setStorageLocation(request.storageLocation() != null ? request.storageLocation().trim() : null);
        record.setTraceabilityCode(traceabilityCode);
        record.setNotes(request.notes() != null ? request.notes().trim() : null);

        HarvestRecord savedRecord = harvestRecordRepository.save(record);
        return mapToResponse(savedRecord);
    }

    @Override
    public PageResponse<HarvestRecordResponse> getHarvestRecordsBySeason(Long userId, Long cropSeasonId, Pageable pageable) {
        CropSeason season = cropSeasonRepository.findById(cropSeasonId)
                .orElseThrow(() -> new ResourceNotFoundException("Mùa vụ canh tác không tồn tại."));

        validateAndGetActiveMember(userId, season);

        Page<HarvestRecord> page = harvestRecordRepository.findByCropSeasonId(cropSeasonId, pageable);
        return PageResponse.map(page, this::mapToResponse);
    }

    @Override
    public HarvestRecordResponse getHarvestRecordById(Long userId, Long harvestRecordId) {
        HarvestRecord record = harvestRecordRepository.findById(harvestRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Bản ghi thu hoạch không tồn tại."));

        validateAndGetActiveMember(userId, record.getCropSeason());

        return mapToResponse(record);
    }

    /**
     * Helper validate active FarmMember & Phân quyền theo FarmRole / StaffAreaAssignment (Tương tự Task 7 & Task 8).
     */
    private FarmMember validateAndGetActiveMember(Long userId, CropSeason season) {
        Farm farm = season.getFarm();
        Plot plot = season.getPlot();
        ProductionArea productionArea = plot != null ? plot.getProductionArea() : null;

        FarmMember member = farmMemberRepository.findByFarmIdAndUserIdAndStatus(farm.getId(), userId, "ACTIVE")
                .orElseThrow(() -> new ForbiddenException("Bạn không có quyền truy cập hoặc ghi nhận thu hoạch cho mùa vụ của trang trại này."));

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

    private HarvestRecordResponse mapToResponse(HarvestRecord record) {
        CropSeason season = record.getCropSeason();
        Farm farm = season.getFarm();
        Plot plot = season.getPlot();

        String recorderName = record.getRecordedByMember() != null && record.getRecordedByMember().getUser() != null
                ? record.getRecordedByMember().getUser().getFullName()
                : null;

        return new HarvestRecordResponse(
                record.getId(),
                season.getId(),
                season.getSeasonCode(),
                season.getSeasonName(),
                farm.getId(),
                farm.getFarmName(),
                plot != null ? plot.getId() : null,
                plot != null ? plot.getPlotCode() : null,
                plot != null ? plot.getPlotName() : null,
                record.getRecordedByMember().getId(),
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
