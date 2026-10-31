package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.risk.CreateZoneRiskReq;
import com.mycropdiary.api.dto.risk.UpdateRiskControlReq;
import com.mycropdiary.api.dto.risk.ZoneRiskResp;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.Zone;
import com.mycropdiary.api.entity.ZoneRiskAssessment;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.ZoneRiskAssessmentRepository;
import com.mycropdiary.api.service.ZoneRiskService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class ZoneRiskServiceImpl implements ZoneRiskService {
    private static final Set<String> VALID_RISK_LEVELS = Set.of("low", "medium", "high", "critical");

    private final ZoneRiskAssessmentRepository riskRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final FarmAccessGuard accessGuard;

    public ZoneRiskServiceImpl(ZoneRiskAssessmentRepository riskRepository,
                               FarmMemberRepository farmMemberRepository,
                               FarmAccessGuard accessGuard) {
        this.riskRepository = riskRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.accessGuard = accessGuard;
    }

    @Override
    public ZoneRiskResp recordZoneRisk(Integer currentUserId, Integer zoneId, CreateZoneRiskReq request) {
        Zone zone = accessGuard.requireZoneAccess(currentUserId, zoneId);
        Integer farmId = zone.getFarm().getId();

        String riskLevel = request.riskLevel().toLowerCase().trim();
        if (!VALID_RISK_LEVELS.contains(riskLevel)) {
            throw new BadRequestException("Mức độ rủi ro không hợp lệ. Chỉ chấp nhận: low, medium, high, critical.");
        }

        FarmMember assessor = farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, currentUserId, "active")
                .orElse(null);

        ZoneRiskAssessment assessment = new ZoneRiskAssessment(
                zone,
                assessor,
                request.riskType().trim(),
                riskLevel,
                request.description() != null ? request.description().trim() : null,
                request.mitigationPlan() != null ? request.mitigationPlan().trim() : null,
                request.imageUrl(),
                request.assessedAt() != null ? request.assessedAt() : Instant.now()
        );

        ZoneRiskAssessment saved = riskRepository.save(assessment);
        return ZoneRiskResp.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ZoneRiskResp> getZoneRiskHistory(Integer currentUserId, Integer zoneId) {
        accessGuard.requireZoneAccess(currentUserId, zoneId);
        List<ZoneRiskAssessment> list = riskRepository.findByZoneIdOrderByAssessedAtDesc(zoneId);
        return list.stream().map(ZoneRiskResp::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ZoneRiskResp> getFarmRiskHistory(Integer currentUserId, Integer farmId) {
        accessGuard.requireFarmAccess(currentUserId, farmId);
        List<ZoneRiskAssessment> list = riskRepository.findByFarmIdOrderByAssessedAtDesc(farmId);
        return list.stream().map(ZoneRiskResp::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ZoneRiskResp getZoneRiskDetails(Integer currentUserId, Integer riskId) {
        ZoneRiskAssessment entity = riskRepository.findById(riskId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bản ghi rủi ro ID: " + riskId));
        accessGuard.requireZoneAccess(currentUserId, entity.getZone().getId());
        return ZoneRiskResp.fromEntity(entity);
    }

    @Override
    public ZoneRiskResp updateRiskControlMeasures(Integer currentUserId, Integer riskId, UpdateRiskControlReq request) {
        ZoneRiskAssessment entity = riskRepository.findById(riskId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bản ghi rủi ro ID: " + riskId));
        accessGuard.requireZoneAccess(currentUserId, entity.getZone().getId());

        if (request.mitigationPlan() == null || request.mitigationPlan().isBlank()) {
            throw new BadRequestException("Biện pháp kiểm soát rủi ro không được để trống.");
        }
        entity.setMitigationPlan(request.mitigationPlan().trim());

        if (request.riskLevel() != null && !request.riskLevel().isBlank()) {
            String updatedLevel = request.riskLevel().toLowerCase().trim();
            if (!VALID_RISK_LEVELS.contains(updatedLevel)) {
                throw new BadRequestException("Mức độ rủi ro không hợp lệ. Chỉ chấp nhận: low, medium, high, critical.");
            }
            entity.setRiskLevel(updatedLevel);
        }

        if (request.description() != null) {
            entity.setDescription(request.description().trim());
        }
        if (request.imageUrl() != null) {
            entity.setImageUrl(request.imageUrl());
        }

        ZoneRiskAssessment updated = riskRepository.save(entity);
        return ZoneRiskResp.fromEntity(updated);
    }

    @Override
    public void deleteZoneRisk(Integer currentUserId, Integer riskId) {
        ZoneRiskAssessment entity = riskRepository.findById(riskId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bản ghi rủi ro ID: " + riskId));
        accessGuard.requireFarmOwner(currentUserId, entity.getZone().getFarm().getId());
        riskRepository.delete(entity);
    }
}
