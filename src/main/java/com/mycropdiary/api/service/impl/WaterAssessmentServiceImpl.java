package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.water.CreateWaterAssessmentReq;
import com.mycropdiary.api.dto.water.UpdateWaterAssessmentReq;
import com.mycropdiary.api.dto.water.WaterAssessmentResp;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.WaterAssessment;
import com.mycropdiary.api.entity.WaterSource;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.WaterAssessmentRepository;
import com.mycropdiary.api.repository.WaterSourceRepository;
import com.mycropdiary.api.service.WaterAssessmentService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class WaterAssessmentServiceImpl implements WaterAssessmentService {

    private final WaterAssessmentRepository waterRepository;
    private final WaterSourceRepository waterSourceRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final FarmAccessGuard accessGuard;

    public WaterAssessmentServiceImpl(WaterAssessmentRepository waterRepository,
                                     WaterSourceRepository waterSourceRepository,
                                     FarmMemberRepository farmMemberRepository,
                                     FarmAccessGuard accessGuard) {
        this.waterRepository = waterRepository;
        this.waterSourceRepository = waterSourceRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.accessGuard = accessGuard;
    }

    @Override
    public WaterAssessmentResp createWaterAssessment(Integer currentUserId, Integer blockId, CreateWaterAssessmentReq request) {
        Block block = accessGuard.requireBlockAccess(currentUserId, blockId);
        Farm farm = block.getZone().getFarm();

        if (request.waterPh() != null && (request.waterPh().compareTo(BigDecimal.ZERO) < 0 || request.waterPh().compareTo(BigDecimal.valueOf(14.0)) > 0)) {
            throw new BadRequestException("Độ pH nước phải nằm trong khoảng từ 0.0 đến 14.0.");
        }

        WaterSource waterSource = null;
        if (request.waterSourceId() != null) {
            waterSource = waterSourceRepository.findById(request.waterSourceId()).orElse(null);
        }
        if (waterSource == null) {
            List<WaterSource> farmSources = waterSourceRepository.findByFarmIdOrderBySourceNameAsc(farm.getId());
            if (!farmSources.isEmpty()) {
                waterSource = farmSources.get(0);
            } else {
                String sourceName = request.waterSourceName() != null && !request.waterSourceName().isBlank()
                        ? request.waterSourceName().trim() : "Nguồn nước chính trang trại";
                String sourceType = request.waterSourceType() != null && !request.waterSourceType().isBlank()
                        ? request.waterSourceType().trim() : "Giếng khoan & Hồ lắng";
                waterSource = waterSourceRepository.save(new WaterSource(farm, sourceName, sourceType, "Nguồn nước cấp hệ thống tưới", null));
            }
        }

        FarmMember assessor = farmMemberRepository.findByFarmIdAndUserIdAndStatus(farm.getId(), currentUserId, "active")
                .orElse(null);

        WaterAssessment assessment = new WaterAssessment(
                waterSource,
                block,
                assessor,
                request.waterPh(),
                request.temperatureC(),
                request.qualityStatus() != null ? request.qualityStatus().trim() : "DAT_CHUAN",
                request.notes() != null ? request.notes().trim() : null,
                request.imageUrl(),
                request.assessedAt() != null ? request.assessedAt() : Instant.now()
        );

        WaterAssessment saved = waterRepository.save(assessment);
        return WaterAssessmentResp.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WaterAssessmentResp> getWaterAssessmentHistory(Integer currentUserId, Integer blockId) {
        accessGuard.requireBlockAccess(currentUserId, blockId);
        List<WaterAssessment> list = waterRepository.findByBlockIdOrderByAssessedAtDesc(blockId);
        return list.stream().map(WaterAssessmentResp::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WaterAssessmentResp> getFarmWaterAssessmentHistory(Integer currentUserId, Integer farmId) {
        accessGuard.requireFarmAccess(currentUserId, farmId);
        List<WaterAssessment> list = waterRepository.findByFarmIdOrderByAssessedAtDesc(farmId);
        return list.stream().map(WaterAssessmentResp::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WaterAssessmentResp getWaterAssessmentDetails(Integer currentUserId, Integer assessmentId) {
        WaterAssessment entity = waterRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kết quả đánh giá nguồn nước ID: " + assessmentId));
        if (entity.getBlock() != null) {
            accessGuard.requireBlockAccess(currentUserId, entity.getBlock().getId());
        } else if (entity.getWaterSource() != null) {
            accessGuard.requireFarmAccess(currentUserId, entity.getWaterSource().getFarm().getId());
        }
        return WaterAssessmentResp.fromEntity(entity);
    }

    @Override
    public WaterAssessmentResp updateWaterAssessment(Integer currentUserId, Integer assessmentId, UpdateWaterAssessmentReq request) {
        WaterAssessment entity = waterRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kết quả đánh giá nguồn nước ID: " + assessmentId));
        if (entity.getBlock() != null) {
            accessGuard.requireBlockAccess(currentUserId, entity.getBlock().getId());
        } else if (entity.getWaterSource() != null) {
            accessGuard.requireFarmAccess(currentUserId, entity.getWaterSource().getFarm().getId());
        }

        if (request.waterPh() != null) {
            if (request.waterPh().compareTo(BigDecimal.ZERO) < 0 || request.waterPh().compareTo(BigDecimal.valueOf(14.0)) > 0) {
                throw new BadRequestException("Độ pH nước phải nằm trong khoảng từ 0.0 đến 14.0.");
            }
            entity.setWaterPh(request.waterPh());
        }
        if (request.temperatureC() != null) {
            entity.setTemperatureC(request.temperatureC());
        }
        if (request.qualityStatus() != null) {
            entity.setQualityStatus(request.qualityStatus().trim());
        }
        if (request.notes() != null) {
            entity.setNotes(request.notes().trim());
        }
        if (request.imageUrl() != null) {
            entity.setImageUrl(request.imageUrl());
        }
        if (request.assessedAt() != null) {
            entity.setAssessedAt(request.assessedAt());
        }
        if (request.waterSourceId() != null) {
            waterSourceRepository.findById(request.waterSourceId()).ifPresent(entity::setWaterSource);
        }

        WaterAssessment saved = waterRepository.save(entity);
        return WaterAssessmentResp.fromEntity(saved);
    }

    @Override
    public void deleteWaterAssessment(Integer currentUserId, Integer assessmentId) {
        WaterAssessment entity = waterRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kết quả đánh giá nguồn nước ID: " + assessmentId));
        Integer farmId = entity.getWaterSource() != null && entity.getWaterSource().getFarm() != null
                ? entity.getWaterSource().getFarm().getId()
                : (entity.getBlock() != null ? entity.getBlock().getZone().getFarm().getId() : null);

        if (farmId != null) {
            accessGuard.requireFarmOwner(currentUserId, farmId);
        }
        waterRepository.delete(entity);
    }
}

