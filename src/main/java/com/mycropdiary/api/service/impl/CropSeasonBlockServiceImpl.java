package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.cultivation.AssignCropSeasonBlockReq;
import com.mycropdiary.api.dto.cultivation.CropSeasonBlockResp;
import com.mycropdiary.api.dto.cultivation.UpdateCropSeasonBlockReq;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import com.mycropdiary.api.entity.cultivation.CropSeasonBlock;
import com.mycropdiary.api.entity.cultivation.CropSeasonBlockId;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceConflictException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.BlockRepository;
import com.mycropdiary.api.repository.cultivation.CropSeasonBlockRepository;
import com.mycropdiary.api.repository.cultivation.CropSeasonRepository;
import com.mycropdiary.api.repository.cultivation.FarmingActivityRepository;
import com.mycropdiary.api.repository.cultivation.HarvestRecordRepository;
import com.mycropdiary.api.service.cultivation.CropSeasonBlockService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class CropSeasonBlockServiceImpl implements CropSeasonBlockService {

    private final CropSeasonBlockRepository cropSeasonBlockRepository;
    private final CropSeasonRepository cropSeasonRepository;
    private final BlockRepository blockRepository;
    private final FarmAccessGuard farmAccessGuard;
    private final FarmingActivityRepository farmingActivityRepository;
    private final HarvestRecordRepository harvestRecordRepository;

    public CropSeasonBlockServiceImpl(CropSeasonBlockRepository cropSeasonBlockRepository,
                                     CropSeasonRepository cropSeasonRepository,
                                     BlockRepository blockRepository,
                                     FarmAccessGuard farmAccessGuard,
                                     FarmingActivityRepository farmingActivityRepository,
                                     HarvestRecordRepository harvestRecordRepository) {
        this.cropSeasonBlockRepository = cropSeasonBlockRepository;
        this.cropSeasonRepository = cropSeasonRepository;
        this.blockRepository = blockRepository;
        this.farmAccessGuard = farmAccessGuard;
        this.farmingActivityRepository = farmingActivityRepository;
        this.harvestRecordRepository = harvestRecordRepository;
    }

    @Override
    public CropSeasonBlockResp assignBlockToSeason(Integer userId, Integer cropSeasonId, AssignCropSeasonBlockReq request) {
        CropSeason season = cropSeasonRepository.findById(cropSeasonId)
                .orElseThrow(() -> new ResourceNotFoundException("Mùa vụ canh tác không tồn tại ID: " + cropSeasonId));

        Integer farmId = season.getFarm().getId();
        farmAccessGuard.requireOwnerOrAdmin(userId, farmId);

        String seasonStatus = season.getSeasonStatus();
        if ("completed".equalsIgnoreCase(seasonStatus) || "cancelled".equalsIgnoreCase(seasonStatus)) {
            throw new BadRequestException("Không thể gán thêm lô đất cho mùa vụ đã kết thúc hoặc đã hủy.");
        }

        Block block = blockRepository.findById(request.blockId())
                .orElseThrow(() -> new ResourceNotFoundException("Lô đất không tồn tại ID: " + request.blockId()));

        if (!block.getZone().getFarm().getId().equals(farmId)) {
            throw new BadRequestException("Lô đất không thuộc cùng trang trại với mùa vụ này.");
        }

        if (cropSeasonBlockRepository.existsByIdCropSeasonIdAndIdBlockId(cropSeasonId, request.blockId())) {
            throw new ResourceConflictException("Lô đất '" + block.getBlockName() + "' đã được gán vào mùa vụ này rồi.");
        }

        LocalDate plantedAt = request.plantedAt() != null ? request.plantedAt() : season.getStartDate();
        LocalDate expectedHarvestAt = request.expectedHarvestAt() != null ? request.expectedHarvestAt() : season.getEndDate();

        if (expectedHarvestAt != null && plantedAt != null && expectedHarvestAt.isBefore(plantedAt)) {
            throw new BadRequestException("Ngày dự kiến thu hoạch không thể trước ngày xuống giống.");
        }

        BigDecimal plantedArea = request.plantedArea() != null ? request.plantedArea() : block.getAreaValue();
        String areaUnit = StringUtils.hasText(request.areaUnit()) ? request.areaUnit().trim() : block.getAreaUnit();

        if (plantedArea != null && block.getAreaValue() != null && plantedArea.compareTo(block.getAreaValue()) > 0) {
            throw new BadRequestException("Diện tích gieo trồng (" + plantedArea + " " + areaUnit +
                    ") không được vượt quá diện tích thực tế của lô đất (" + block.getAreaValue() + " " + block.getAreaUnit() + ").");
        }

        CropSeasonBlock csb = new CropSeasonBlock(season, block, plantedArea, areaUnit, plantedAt, expectedHarvestAt);
        CropSeasonBlock saved = cropSeasonBlockRepository.save(csb);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CropSeasonBlockResp> getBlocksBySeason(Integer userId, Integer cropSeasonId) {
        CropSeason season = cropSeasonRepository.findById(cropSeasonId)
                .orElseThrow(() -> new ResourceNotFoundException("Mùa vụ canh tác không tồn tại ID: " + cropSeasonId));

        farmAccessGuard.requireFarmMember(userId, season.getFarm().getId());

        return cropSeasonBlockRepository.findByCropSeasonIdWithDetails(cropSeasonId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CropSeasonBlockResp getBlockInSeason(Integer userId, Integer cropSeasonId, Integer blockId) {
        CropSeasonBlock csb = cropSeasonBlockRepository.findByCropSeasonIdAndBlockIdWithDetails(cropSeasonId, blockId)
                .orElseThrow(() -> new ResourceNotFoundException("Lô đất ID " + blockId + " chưa được gán vào mùa vụ ID " + cropSeasonId));

        farmAccessGuard.requireFarmMember(userId, csb.getCropSeason().getFarm().getId());

        return mapToResponse(csb);
    }

    @Override
    public CropSeasonBlockResp updateBlockInSeason(Integer userId, Integer cropSeasonId, Integer blockId, UpdateCropSeasonBlockReq request) {
        CropSeasonBlock csb = cropSeasonBlockRepository.findByCropSeasonIdAndBlockIdWithDetails(cropSeasonId, blockId)
                .orElseThrow(() -> new ResourceNotFoundException("Lô đất ID " + blockId + " chưa được gán vào mùa vụ ID " + cropSeasonId));

        Integer farmId = csb.getCropSeason().getFarm().getId();
        farmAccessGuard.requireOwnerOrAdmin(userId, farmId);

        String seasonStatus = csb.getCropSeason().getSeasonStatus();
        if ("completed".equalsIgnoreCase(seasonStatus) || "cancelled".equalsIgnoreCase(seasonStatus)) {
            throw new BadRequestException("Không thể chỉnh sửa thông tin lô đất của mùa vụ đã kết thúc hoặc đã hủy.");
        }

        LocalDate plantedAt = request.plantedAt() != null ? request.plantedAt() : csb.getPlantedAt();
        LocalDate expectedHarvestAt = request.expectedHarvestAt() != null ? request.expectedHarvestAt() : csb.getExpectedHarvestAt();

        if (expectedHarvestAt != null && plantedAt != null && expectedHarvestAt.isBefore(plantedAt)) {
            throw new BadRequestException("Ngày dự kiến thu hoạch không thể trước ngày xuống giống.");
        }

        Block block = csb.getBlock();
        BigDecimal plantedArea = request.plantedArea() != null ? request.plantedArea() : csb.getPlantedArea();
        String areaUnit = StringUtils.hasText(request.areaUnit()) ? request.areaUnit().trim() : csb.getAreaUnit();

        if (plantedArea != null && block.getAreaValue() != null && plantedArea.compareTo(block.getAreaValue()) > 0) {
            throw new BadRequestException("Diện tích gieo trồng (" + plantedArea + " " + areaUnit +
                    ") không được vượt quá diện tích thực tế của lô đất (" + block.getAreaValue() + " " + block.getAreaUnit() + ").");
        }

        csb.setPlantedArea(plantedArea);
        csb.setAreaUnit(areaUnit);
        csb.setPlantedAt(plantedAt);
        csb.setExpectedHarvestAt(expectedHarvestAt);

        CropSeasonBlock saved = cropSeasonBlockRepository.save(csb);
        return mapToResponse(saved);
    }

    @Override
    public void removeBlockFromSeason(Integer userId, Integer cropSeasonId, Integer blockId) {
        CropSeasonBlock csb = cropSeasonBlockRepository.findByCropSeasonIdAndBlockIdWithDetails(cropSeasonId, blockId)
                .orElseThrow(() -> new ResourceNotFoundException("Lô đất ID " + blockId + " chưa được gán vào mùa vụ ID " + cropSeasonId));

        Integer farmId = csb.getCropSeason().getFarm().getId();
        farmAccessGuard.requireOwnerOrAdmin(userId, farmId);

        String seasonStatus = csb.getCropSeason().getSeasonStatus();
        if ("completed".equalsIgnoreCase(seasonStatus) || "cancelled".equalsIgnoreCase(seasonStatus)) {
            throw new BadRequestException("Không thể gỡ lô đất của mùa vụ đã kết thúc hoặc đã hủy.");
        }

        boolean hasActivities = farmingActivityRepository.existsByCropSeasonIdAndBlockId(cropSeasonId, blockId);
        boolean hasHarvests = harvestRecordRepository.existsByCropSeasonIdAndBlockId(cropSeasonId, blockId);

        if (hasActivities || hasHarvests) {
            throw new BadRequestException("Không thể gỡ lô đất khỏi mùa vụ vì đã có nhật ký canh tác hoặc thu hoạch liên kết tại lô này.");
        }

        cropSeasonBlockRepository.delete(csb);
    }

    private CropSeasonBlockResp mapToResponse(CropSeasonBlock csb) {
        CropSeason season = csb.getCropSeason();
        Block block = csb.getBlock();

        Integer seasonId = season != null ? season.getId() : null;
        String seasonName = season != null ? season.getSeasonName() : null;
        Integer blockId = block != null ? block.getId() : null;
        String blockName = block != null ? block.getBlockName() : null;
        Integer zoneId = (block != null && block.getZone() != null) ? block.getZone().getId() : null;
        String zoneName = (block != null && block.getZone() != null) ? block.getZone().getZoneName() : null;
        BigDecimal blockArea = block != null ? block.getAreaValue() : null;
        String blockAreaUnit = block != null ? block.getAreaUnit() : null;
        String soilType = block != null ? block.getSoilType() : null;

        return new CropSeasonBlockResp(
                seasonId,
                seasonName,
                blockId,
                blockName,
                zoneId,
                zoneName,
                blockArea,
                blockAreaUnit,
                soilType,
                csb.getPlantedArea(),
                csb.getAreaUnit(),
                csb.getPlantedAt(),
                csb.getExpectedHarvestAt()
        );
    }
}
