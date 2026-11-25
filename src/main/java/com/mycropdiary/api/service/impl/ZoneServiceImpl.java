package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.assignment.AssignedZoneRespDTO;
import com.mycropdiary.api.dto.block.BlockRespDTO;
import com.mycropdiary.api.dto.farm.CreateZoneReqDTO;
import com.mycropdiary.api.dto.farm.UpdateZoneReqDTO;
import com.mycropdiary.api.dto.farm.ZoneDetailRespDTO;
import com.mycropdiary.api.dto.farm.ZoneRespDTO;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.StaffZoneAssignment;
import com.mycropdiary.api.entity.Zone;
import com.mycropdiary.api.exception.BusinessRuleException;
import com.mycropdiary.api.exception.ResourceConflictException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.BlockRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.StaffZoneAssignmentRepository;
import com.mycropdiary.api.repository.ZoneRepository;
import com.mycropdiary.api.service.ZoneAssignmentService;
import com.mycropdiary.api.mapper.BlockMapper;
import com.mycropdiary.api.mapper.ZoneMapper;
import com.mycropdiary.api.service.ZoneService;
import com.mycropdiary.api.util.FarmAccessGuard;
import com.mycropdiary.api.util.GeoAreaUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class ZoneServiceImpl implements ZoneService {
    private final ZoneRepository zoneRepository;
    private final FarmRepository farmRepository;
    private final BlockRepository blockRepository;
    private final StaffZoneAssignmentRepository staffZoneAssignmentRepository;
    private final ZoneAssignmentService zoneAssignmentService;
    private final FarmAccessGuard accessGuard;

    public ZoneServiceImpl(ZoneRepository zoneRepository,
                           FarmRepository farmRepository,
                           BlockRepository blockRepository,
                           StaffZoneAssignmentRepository staffZoneAssignmentRepository,
                           ZoneAssignmentService zoneAssignmentService,
                           FarmAccessGuard accessGuard) {
        this.zoneRepository = zoneRepository;
        this.farmRepository = farmRepository;
        this.blockRepository = blockRepository;
        this.staffZoneAssignmentRepository = staffZoneAssignmentRepository;
        this.zoneAssignmentService = zoneAssignmentService;
        this.accessGuard = accessGuard;
    }

    @Override
    public ZoneRespDTO createZone(Integer currentUserId, Integer farmId, CreateZoneReqDTO request) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trang trại ID: " + farmId));

        accessGuard.requireOwnerOrAdmin(currentUserId, farmId);

        String zoneName = request.zoneName().trim();
        if (zoneRepository.existsByFarmIdAndZoneNameIgnoreCase(farmId, zoneName)) {
            throw new ResourceConflictException("Tên phân khu (Zone) '" + zoneName + "' đã tồn tại trong trang trại này");
        }
        if (request.areaValue() != null && request.areaValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Diện tích phân khu phải lớn hơn 0.");
        }
        validateFarmAreaCapacity(farm, request.areaValue(), request.areaUnit(), null);

        Zone zone = new Zone(farm, zoneName, request.areaValue(), request.description());
        if (request.areaUnit() != null) {
            zone.setAreaUnit(request.areaUnit());
        }
        if (request.imageUrl() != null) {
            zone.setImageUrl(request.imageUrl());
        }

        Zone saved = zoneRepository.save(zone);
        return toZoneResponse(saved);
    }

    @Override
    public ZoneRespDTO updateZone(Integer currentUserId, Integer zoneId, UpdateZoneReqDTO request) {
        Zone zone = accessGuard.requireZoneAccess(currentUserId, zoneId);
        accessGuard.requireOwnerOrAdmin(currentUserId, zone.getFarm().getId());

        String zoneName = request.zoneName().trim();
        if (zoneRepository.existsByFarmIdAndZoneNameIgnoreCaseAndIdNot(zone.getFarm().getId(), zoneName, zoneId)) {
            throw new ResourceConflictException("Tên phân khu (Zone) '" + zoneName + "' đã được sử dụng bởi phân khu khác");
        }

        if (request.areaValue() != null && request.areaValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Diện tích phân khu phải lớn hơn 0.");
        }

        String effectiveUnit = request.areaUnit() != null ? request.areaUnit() : zone.getAreaUnit();
        validateFarmAreaCapacity(zone.getFarm(), request.areaValue(), effectiveUnit, zoneId);

        // Business Rule: Diện tích phân khu mới không được nhỏ hơn tổng diện tích các lô đất hiện có trực thuộc (quy đổi m²)
        if (request.areaValue() != null && request.areaValue().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal newZoneAreaM2 = GeoAreaUtils.toSquareMeters(request.areaValue(), effectiveUnit);
            List<Block> existingBlocks = blockRepository.findByZoneId(zoneId);
            BigDecimal currentBlocksTotalM2 = existingBlocks != null
                    ? existingBlocks.stream()
                        .map(b -> GeoAreaUtils.toSquareMeters(b.getAreaValue(), b.getAreaUnit()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                    : BigDecimal.ZERO;
            if (currentBlocksTotalM2.compareTo(newZoneAreaM2) > 0) {
                throw new BusinessRuleException("Diện tích phân khu mới (" + newZoneAreaM2.stripTrailingZeros().toPlainString()
                        + " m²) không được nhỏ hơn tổng diện tích các lô đất hiện có (" + currentBlocksTotalM2.stripTrailingZeros().toPlainString() + " m²).");
            }
        }

        zone.setZoneName(zoneName);
        zone.setDescription(request.description());
        zone.setImageUrl(request.imageUrl());
        zone.setAreaValue(request.areaValue());
        if (request.areaUnit() != null) {
            zone.setAreaUnit(request.areaUnit());
        }

        Zone saved = zoneRepository.save(zone);
        return toZoneResponse(saved);
    }

    @Override
    public void deleteZone(Integer currentUserId, Integer zoneId) {
        Zone zone = accessGuard.requireZoneAccess(currentUserId, zoneId);
        accessGuard.requireOwnerOrAdmin(currentUserId, zone.getFarm().getId());

        // Kiểm tra nếu zone còn chứa blocks thì không cho xóa trực tiếp hoặc cảnh báo
        List<Block> blocks = blockRepository.findByZoneId(zoneId);
        if (!blocks.isEmpty()) {
            throw new BusinessRuleException("Không thể xóa phân khu (Zone) vì vẫn còn " + blocks.size() + " lô đất (Block) trực thuộc.");
        }

        zoneRepository.delete(zone);
    }

    @Override
    @Transactional(readOnly = true)
    public ZoneDetailRespDTO getZoneById(Integer currentUserId, Integer zoneId) {
        Zone zone = accessGuard.requireZoneAccess(currentUserId, zoneId);
        List<Block> blocks = blockRepository.findByZoneId(zoneId);
        List<BlockRespDTO> blockResps = blocks.stream().map(this::toBlockResponse).toList();

        return new ZoneDetailRespDTO(
                zone.getId(),
                zone.getFarm().getId(),
                zone.getZoneName(),
                zone.getAreaValue(),
                zone.getAreaUnit(),
                zone.getDescription(),
                zone.getImageUrl(),
                "active",
                zone.getCreatedAt(),
                zone.getCreatedAt(),
                blockResps.size(),
                blockResps
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ZoneRespDTO> searchZones(Integer currentUserId, Integer farmId, String keyword, String status) {
        FarmMember member = accessGuard.requireFarmMember(currentUserId, farmId);
        String role = accessGuard.getUserRoleInFarm(currentUserId, farmId);

        String trimmedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        List<Zone> zones;

        if ("owner".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role)) {
            zones = zoneRepository.searchZones(farmId, trimmedKeyword);
        } else {
            // Nhân viên: chỉ lấy những Zone mình được phân công
            List<StaffZoneAssignment> assignments = staffZoneAssignmentRepository.findByFarmMemberIdAndEndedAtIsNull(member.getId());
            zones = assignments.stream()
                    .map(StaffZoneAssignment::getZone)
                    .filter(z -> trimmedKeyword == null || z.getZoneName().toLowerCase().contains(trimmedKeyword.toLowerCase()))
                    .toList();
        }

        return zones.stream().map(this::toZoneResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignedZoneRespDTO> getAssignedZones(Integer currentUserId, Integer farmId) {
        return zoneAssignmentService.getAssignedZonesForCurrentStaff(currentUserId, farmId);
    }

    @Override
    @Transactional(readOnly = true)
    public ZoneDetailRespDTO getAssignedZoneDetail(Integer currentUserId, Integer zoneId) {
        return getZoneById(currentUserId, zoneId);
    }

    private void validateFarmAreaCapacity(Farm farm, BigDecimal newZoneArea, String newZoneUnit, Integer excludeZoneId) {
        if (farm == null || newZoneArea == null || newZoneArea.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        double farmAreaM2 = GeoAreaUtils.calculatePolygonAreaM2(farm.getBoundaryCoordinates());
        if (farmAreaM2 <= 0.0) {
            return;
        }
        List<Zone> farmZones = zoneRepository.findByFarmId(farm.getId());
        BigDecimal existingZonesM2 = farmZones != null
                ? farmZones.stream()
                    .filter(z -> excludeZoneId == null || !excludeZoneId.equals(z.getId()))
                    .map(z -> GeoAreaUtils.toSquareMeters(z.getAreaValue(), z.getAreaUnit()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;
        BigDecimal newZoneM2 = GeoAreaUtils.toSquareMeters(newZoneArea, newZoneUnit);
        BigDecimal totalZonesM2 = existingZonesM2.add(newZoneM2);
        if (totalZonesM2.compareTo(BigDecimal.valueOf(farmAreaM2)) > 0) {
            throw new BusinessRuleException("Tổng diện tích các phân khu (" + totalZonesM2.stripTrailingZeros().toPlainString()
                    + " m²) không được vượt quá diện tích trang trại (" + Math.round(farmAreaM2) + " m²).");
        }
    }

    private final ZoneMapper zoneMapper = new ZoneMapper();
    private final BlockMapper blockMapper = new BlockMapper();

    private ZoneRespDTO toZoneResponse(Zone zone) {
        return zoneMapper.toZoneResponse(zone);
    }

    private BlockRespDTO toBlockResponse(Block b) {
        return blockMapper.toBlockResponse(b);
    }
}
