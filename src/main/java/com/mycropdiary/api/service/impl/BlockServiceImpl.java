package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.block.CreateBlockReq;
import com.mycropdiary.api.dto.block.BlockResp;
import com.mycropdiary.api.dto.block.UpdateBlockReq;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.StaffZoneAssignment;
import com.mycropdiary.api.entity.Zone;
import com.mycropdiary.api.exception.BusinessRuleException;
import com.mycropdiary.api.exception.ResourceConflictException;
import com.mycropdiary.api.repository.BlockRepository;
import com.mycropdiary.api.repository.StaffZoneAssignmentRepository;
import com.mycropdiary.api.repository.ZoneRepository;
import com.mycropdiary.api.service.BlockService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service xử lý nghiệp vụ quản lý Lô / Thửa đất (Block).
 */
@Service
@Transactional
public class BlockServiceImpl implements BlockService {
    private final BlockRepository blockRepository;
    private final ZoneRepository zoneRepository;
    private final StaffZoneAssignmentRepository assignmentRepository;
    private final FarmAccessGuard accessGuard;

    public BlockServiceImpl(BlockRepository blockRepository,
                            ZoneRepository zoneRepository,
                            StaffZoneAssignmentRepository assignmentRepository,
                            FarmAccessGuard accessGuard) {
        this.blockRepository = blockRepository;
        this.zoneRepository = zoneRepository;
        this.assignmentRepository = assignmentRepository;
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

        // Business Rule: Tổng diện tích các thửa không vượt quá diện tích khu vực sản xuất (nếu có giới hạn)
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

        // Business Rule: Kiểm tra diện tích khu vực
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
        FarmMember currentMember = accessGuard.requireFarmAccess(currentUserId, farmId);

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
        // Business Rule: Kiểm tra quyền truy cập thửa đất
        Block block = accessGuard.requireBlockAccess(currentUserId, blockId);
        blockRepository.delete(block);
    }

    private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if ((latitude != null && longitude == null) || (latitude == null && longitude != null)) {
            throw new BusinessRuleException("Tọa độ phải bao gồm đầy đủ cả Vĩ độ (Latitude) và Kinh độ (Longitude)");
        }
    }

    private void validateZoneCapacity(Zone zone, BigDecimal newBlockArea, Integer excludeBlockId) {
        if (zone.getAreaValue() != null && zone.getAreaValue().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentTotal = blockRepository.sumAreaValueByZoneId(zone.getId(), excludeBlockId);
            BigDecimal totalWithNew = currentTotal.add(newBlockArea);
            if (totalWithNew.compareTo(zone.getAreaValue()) > 0) {
                throw new BusinessRuleException("Tổng diện tích các thửa đất (" + totalWithNew + " " + zone.getAreaUnit() + ") vượt quá diện tích của khu vực sản xuất (" + zone.getAreaValue() + " " + zone.getAreaUnit() + ")");
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
}
