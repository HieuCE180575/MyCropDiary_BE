package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.block.BlockConditionResp;
import com.mycropdiary.api.dto.block.BlockResp;
import com.mycropdiary.api.dto.block.CreateBlockReq;
import com.mycropdiary.api.dto.block.RecordBlockConditionReq;
import com.mycropdiary.api.dto.block.UpdateBlockReq;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.BlockConditionObservation;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.StaffZoneAssignment;
import com.mycropdiary.api.entity.Zone;
import com.mycropdiary.api.exception.BusinessRuleException;
import com.mycropdiary.api.exception.ResourceConflictException;
import com.mycropdiary.api.repository.BlockConditionObservationRepository;
import com.mycropdiary.api.repository.BlockRepository;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.StaffZoneAssignmentRepository;
import com.mycropdiary.api.repository.ZoneRepository;
import com.mycropdiary.api.service.BlockService;
import com.mycropdiary.api.entity.cultivation.CropSeasonBlock;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.repository.cultivation.CropSeasonBlockRepository;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service xử lý nghiệp vụ quản lý Lô / Thửa đất (Block) - UC-17.
 */
@Service
@Transactional
public class BlockServiceImpl implements BlockService {
    private final BlockRepository blockRepository;
    private final ZoneRepository zoneRepository;
    private final StaffZoneAssignmentRepository assignmentRepository;
    private final BlockConditionObservationRepository conditionObservationRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final FarmAccessGuard accessGuard;
    private final CropSeasonBlockRepository cropSeasonBlockRepository;

    public BlockServiceImpl(BlockRepository blockRepository,
                            ZoneRepository zoneRepository,
                            StaffZoneAssignmentRepository assignmentRepository,
                            BlockConditionObservationRepository conditionObservationRepository,
                            FarmMemberRepository farmMemberRepository,
                            FarmAccessGuard accessGuard) {
        this(blockRepository, zoneRepository, assignmentRepository, conditionObservationRepository, farmMemberRepository, accessGuard, null);
    }

    public BlockServiceImpl(BlockRepository blockRepository,
                            ZoneRepository zoneRepository,
                            StaffZoneAssignmentRepository assignmentRepository,
                            BlockConditionObservationRepository conditionObservationRepository,
                            FarmMemberRepository farmMemberRepository,
                            FarmAccessGuard accessGuard,
                            CropSeasonBlockRepository cropSeasonBlockRepository) {
        this.cropSeasonBlockRepository = cropSeasonBlockRepository;
        this.blockRepository = blockRepository;
        this.zoneRepository = zoneRepository;
        this.assignmentRepository = assignmentRepository;
        this.conditionObservationRepository = conditionObservationRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.accessGuard = accessGuard;
    }

    @Override
    public BlockResp createBlock(Integer currentUserId, Integer zoneId, CreateBlockReq request) {
        // Business Rule: Kiểm tra quyền truy cập khu vực (chỉ Owner hoặc Staff được phân công khu vực này)
        Zone zone = accessGuard.requireZoneAccess(currentUserId, zoneId);

        String blockName = request.blockName().trim();
        // Business Rule: Tên thửa đất/lô phải là duy nhất trong cùng khu vực sản xuất
        if (blockRepository.existsByZoneIdAndBlockNameIgnoreCase(zoneId, blockName)) {
            throw new ResourceConflictException("Tên lô / thửa đất '" + blockName + "' đã tồn tại trong khu vực sản xuất này");
        }

        // Business Rule: Kiểm tra tọa độ nếu có
        validateCoordinates(request.latitude(), request.longitude());

        // Business Rule: Diện tích lô không lớn hơn khu vực và Tổng diện tích các thửa không vượt quá diện tích khu vực
        validateZoneCapacity(zone, request.areaValue(), null);

        Block block = new Block();
        block.setZone(zone);
        block.setBlockName(blockName);
        block.setDescription(request.description());
        block.setImageUrl(request.imageUrl());
        block.setAreaValue(request.areaValue());
        block.setAreaUnit(request.areaUnit() != null ? request.areaUnit() : "m2");
        block.setSoilType(request.soilType());

        Block saved = blockRepository.save(block);
        return toBlockResponse(saved);
    }

    @Override
    public BlockResp updateBlock(Integer currentUserId, Integer blockId, UpdateBlockReq request) {
        // Business Rule: Kiểm tra quyền truy cập thửa đất
        Block block = accessGuard.requireBlockAccess(currentUserId, blockId);
        Zone zone = block.getZone();

        String blockName = request.blockName().trim();
        // Business Rule: Kiểm tra trùng tên (loại trừ chính nó)
        if (blockRepository.existsByZoneIdAndBlockNameIgnoreCaseAndIdNot(zone.getId(), blockName, blockId)) {
            throw new ResourceConflictException("Tên lô / thửa đất '" + blockName + "' đã tồn tại trong khu vực sản xuất này");
        }

        // Business Rule: Kiểm tra tọa độ
        validateCoordinates(request.latitude(), request.longitude());

        // Business Rule: Diện tích lô không lớn hơn khu vực và Tổng diện tích các thửa không vượt quá diện tích khu vực
        validateZoneCapacity(zone, request.areaValue(), blockId);

        block.setBlockName(blockName);
        block.setDescription(request.description());
        block.setImageUrl(request.imageUrl());
        block.setAreaValue(request.areaValue());
        if (request.areaUnit() != null) {
            block.setAreaUnit(request.areaUnit());
        }
        if (request.soilType() != null) {
            block.setSoilType(request.soilType());
        }

        Block saved = blockRepository.save(block);
        return toBlockResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BlockResp getBlockById(Integer currentUserId, Integer blockId) {
        // Business Rule: Kiểm tra quyền truy cập thửa đất
        Block block = accessGuard.requireBlockAccess(currentUserId, blockId);
        return toBlockResponse(block);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlockResp> searchBlocks(Integer currentUserId, Integer farmId, Integer zoneId, String status, String keyword) {
        // Business Rule: Kiểm tra quyền truy cập vào trang trại
        FarmMember currentMember = accessGuard.requireFarmMember(currentUserId, farmId);

        Set<Integer> allowedZoneIds = null;
        // Chặn truy cập trái quyền đối với Staff:
        if (currentMember != null && !currentMember.isOwner()) {
            List<StaffZoneAssignment> staffAssignments = assignmentRepository.findActiveAssignmentsByMemberId(currentMember.getId());
            allowedZoneIds = staffAssignments.stream()
                    .map(a -> a.getZone().getId())
                    .collect(Collectors.toSet());

            if (zoneId != null) {
                if (!allowedZoneIds.contains(zoneId)) {
                    throw new org.springframework.security.access.AccessDeniedException(
                            "Nhân viên không có quyền truy cập thửa đất thuộc khu vực này");
                }
            } else {
                if (allowedZoneIds.isEmpty()) {
                    return List.of();
                }
            }
        }

        String trimmedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        List<Block> blocks = blockRepository.searchBlocks(farmId, zoneId, trimmedKeyword, allowedZoneIds);

        return blocks.stream().map(this::toBlockResponse).toList();
    }

    @Override
    public void deleteBlock(Integer currentUserId, Integer blockId) {
        // Business Rule: Delete Eligible Block - Kiểm tra quyền và điều kiện hợp lệ của lô đất
        Block block = accessGuard.requireBlockAccess(currentUserId, blockId);

        // 1. Kiểm tra phân bổ mùa vụ: Không được xóa lô đang trong mùa vụ hoạt động / canh tác
        List<CropSeasonBlock> seasonBlocks = cropSeasonBlockRepository != null ? cropSeasonBlockRepository.findByIdBlockId(blockId) : List.of();
        for (CropSeasonBlock csb : seasonBlocks) {
            String status = csb.getCropSeason() != null ? csb.getCropSeason().getSeasonStatus() : null;
            if (status != null && (status.equalsIgnoreCase("active") || status.equalsIgnoreCase("in_progress") || status.equalsIgnoreCase("planned"))) {
                throw new BadRequestException("Lô đất '" + block.getBlockName() + "' không đủ điều kiện xóa vì đang được phân bổ trong mùa vụ '" + csb.getCropSeason().getSeasonName() + "' (trạng thái: " + status + "). Vui lòng gỡ lô đất hoặc hoàn thành mùa vụ trước.");
            }
        }

        // 2. Dọn sạch các liên kết mùa vụ đã kết thúc / hủy của lô đất này
        for (CropSeasonBlock csb : seasonBlocks) {
            cropSeasonBlockRepository.delete(csb);
        }

        // 3. Tiến hành xóa lô đất hợp lệ
        blockRepository.delete(block);
    }

    @Override
    public BlockConditionResp recordBlockCondition(Integer currentUserId, Integer blockId, RecordBlockConditionReq request) {
        Block block = accessGuard.requireBlockAccess(currentUserId, blockId);
        Integer farmId = block.getZone().getFarm().getId();
        FarmMember recorder = farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, currentUserId, "active").orElse(null);

        BlockConditionObservation observation = new BlockConditionObservation(
                block,
                recorder,
                request.conditionType(),
                request.description(),
                request.imageUrl(),
                request.observedAt() != null ? request.observedAt() : Instant.now()
        );

        BlockConditionObservation saved = conditionObservationRepository.save(observation);
        return toBlockConditionResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlockConditionResp> getBlockConditions(Integer currentUserId, Integer blockId) {
        accessGuard.requireBlockAccess(currentUserId, blockId);
        List<BlockConditionObservation> list = conditionObservationRepository.findByBlockIdOrderByObservedAtDesc(blockId);
        return list.stream().map(this::toBlockConditionResponse).toList();
    }

    private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if ((latitude != null && longitude == null) || (latitude == null && longitude != null)) {
            throw new BusinessRuleException("Tọa độ phải bao gồm đầy đủ cả Vĩ độ (Latitude) và Kinh độ (Longitude)");
        }
    }

    private void validateZoneCapacity(Zone zone, BigDecimal newBlockArea, Integer excludeBlockId) {
        if (zone.getAreaValue() != null && zone.getAreaValue().compareTo(BigDecimal.ZERO) > 0) {
            String unit = zone.getAreaUnit() != null ? zone.getAreaUnit() : "m2";
            if (newBlockArea != null && newBlockArea.compareTo(zone.getAreaValue()) > 0) {
                throw new BusinessRuleException("Diện tích lô đất (" + newBlockArea + " " + unit + ") không được lớn hơn diện tích của phân khu (" + zone.getAreaValue() + " " + unit + ")");
            }
            BigDecimal currentTotal = blockRepository.sumAreaValueByZoneId(zone.getId(), excludeBlockId);
            BigDecimal totalWithNew = currentTotal.add(newBlockArea != null ? newBlockArea : BigDecimal.ZERO);
            if (totalWithNew.compareTo(zone.getAreaValue()) > 0) {
                throw new BusinessRuleException("Tổng diện tích các thửa đất (" + totalWithNew + " " + unit + ") vượt quá diện tích của khu vực sản xuất (" + zone.getAreaValue() + " " + unit + ")");
            }
        }
    }

    private BlockResp toBlockResponse(Block b) {
        return new BlockResp(
                b.getId(),
                b.getZone().getId(),
                b.getZone().getZoneName(),
                b.getZone().getFarm().getId(),
                b.getBlockName(),
                b.getAreaValue(),
                b.getAreaUnit(),
                b.getSoilType(),
                b.getDescription(),
                b.getImageUrl(),
                "AVAILABLE",
                null,
                null,
                null,
                b.getCreatedAt()
        );
    }

    private BlockConditionResp toBlockConditionResponse(BlockConditionObservation obs) {
        Integer recorderId = obs.getRecordedBy() != null ? obs.getRecordedBy().getId() : null;
        String recorderName = (obs.getRecordedBy() != null && obs.getRecordedBy().getUser() != null)
                ? obs.getRecordedBy().getUser().getFullName() : null;

        return new BlockConditionResp(
                obs.getId(),
                obs.getBlock().getId(),
                obs.getBlock().getBlockName(),
                recorderId,
                recorderName,
                obs.getConditionType(),
                obs.getDescription(),
                obs.getImageUrl(),
                obs.getObservedAt()
        );
    }
}
