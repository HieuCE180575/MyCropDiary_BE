package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.assignment.AssignedZoneResp;
import com.mycropdiary.api.dto.block.BlockResp;
import com.mycropdiary.api.dto.farm.CreateZoneReq;
import com.mycropdiary.api.dto.farm.UpdateZoneReq;
import com.mycropdiary.api.dto.farm.ZoneDetailResp;
import com.mycropdiary.api.dto.farm.ZoneResp;
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
import com.mycropdiary.api.service.ZoneService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public ZoneResp createZone(Integer currentUserId, Integer farmId, CreateZoneReq request) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trang trại ID: " + farmId));

        accessGuard.requireOwnerOrAdmin(currentUserId, farmId);

        String zoneName = request.zoneName().trim();
        if (zoneRepository.existsByFarmIdAndZoneNameIgnoreCase(farmId, zoneName)) {
            throw new ResourceConflictException("Tên phân khu (Zone) '" + zoneName + "' đã tồn tại trong trang trại này");
        }

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
    public ZoneResp updateZone(Integer currentUserId, Integer zoneId, UpdateZoneReq request) {
        Zone zone = accessGuard.requireZoneAccess(currentUserId, zoneId);
        accessGuard.requireOwnerOrAdmin(currentUserId, zone.getFarm().getId());

        String zoneName = request.zoneName().trim();
        if (zoneRepository.existsByFarmIdAndZoneNameIgnoreCaseAndIdNot(zone.getFarm().getId(), zoneName, zoneId)) {
            throw new ResourceConflictException("Tên phân khu (Zone) '" + zoneName + "' đã được sử dụng bởi phân khu khác");
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
    public ZoneDetailResp getZoneById(Integer currentUserId, Integer zoneId) {
        Zone zone = accessGuard.requireZoneAccess(currentUserId, zoneId);
        List<Block> blocks = blockRepository.findByZoneId(zoneId);
        List<BlockResp> blockResps = blocks.stream().map(this::toBlockResponse).toList();

        return new ZoneDetailResp(
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
    public List<ZoneResp> searchZones(Integer currentUserId, Integer farmId, String keyword, String status) {
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
    public List<AssignedZoneResp> getAssignedZones(Integer currentUserId, Integer farmId) {
        return zoneAssignmentService.getAssignedZonesForCurrentStaff(currentUserId, farmId);
    }

    @Override
    @Transactional(readOnly = true)
    public ZoneDetailResp getAssignedZoneDetail(Integer currentUserId, Integer zoneId) {
        return getZoneById(currentUserId, zoneId);
    }

    private ZoneResp toZoneResponse(Zone zone) {
        return new ZoneResp(
                zone.getId(),
                zone.getFarm().getId(),
                zone.getZoneName(),
                zone.getAreaValue(),
                zone.getAreaUnit(),
                zone.getDescription(),
                zone.getImageUrl(),
                "active",
                zone.getCreatedAt(),
                zone.getCreatedAt()
        );
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
