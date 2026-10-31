package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.soil.CreateSoilAssessmentReq;
import com.mycropdiary.api.dto.soil.SoilAssessmentResp;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.SoilAssessment;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.SoilAssessmentRepository;
import com.mycropdiary.api.service.SoilAssessmentService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class SoilAssessmentServiceImpl implements SoilAssessmentService {
    private final SoilAssessmentRepository soilRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final FarmAccessGuard accessGuard;

    public SoilAssessmentServiceImpl(SoilAssessmentRepository soilRepository,
                                    FarmMemberRepository farmMemberRepository,
                                    FarmAccessGuard accessGuard) {
        this.soilRepository = soilRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.accessGuard = accessGuard;
    }

    @Override
    public SoilAssessmentResp createSoilAssessment(Integer currentUserId, Integer blockId, CreateSoilAssessmentReq request) {
        Block block = accessGuard.requireBlockAccess(currentUserId, blockId);
        Integer farmId = block.getZone().getFarm().getId();

        if (request.soilPh() != null && (request.soilPh().compareTo(BigDecimal.ZERO) < 0 || request.soilPh().compareTo(BigDecimal.valueOf(14.0)) > 0)) {
            throw new BadRequestException("Độ pH đất phải nằm trong khoảng từ 0.0 đến 14.0.");
        }
        if (request.moisturePercent() != null && (request.moisturePercent().compareTo(BigDecimal.ZERO) < 0 || request.moisturePercent().compareTo(BigDecimal.valueOf(100.0)) > 0)) {
            throw new BadRequestException("Độ ẩm đất phải nằm trong khoảng từ 0% đến 100%.");
        }

        FarmMember assessor = farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, currentUserId, "active")
                .orElse(null);

        SoilAssessment assessment = new SoilAssessment(
                block,
                assessor,
                request.soilPh(),
                request.moisturePercent(),
                request.nutrientData() != null && !request.nutrientData().isBlank() ? request.nutrientData() : "{}",
                request.notes() != null ? request.notes().trim() : null,
                request.imageUrl(),
                request.assessedAt() != null ? request.assessedAt() : Instant.now()
        );

        SoilAssessment saved = soilRepository.save(assessment);
        return SoilAssessmentResp.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SoilAssessmentResp> getSoilAssessmentHistory(Integer currentUserId, Integer blockId) {
        accessGuard.requireBlockAccess(currentUserId, blockId);
        List<SoilAssessment> list = soilRepository.findByBlockIdOrderByAssessedAtDesc(blockId);
        return list.stream().map(SoilAssessmentResp::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SoilAssessmentResp> getFarmSoilAssessmentHistory(Integer currentUserId, Integer farmId) {
        accessGuard.requireFarmAccess(currentUserId, farmId);
        List<SoilAssessment> list = soilRepository.findByFarmIdOrderByAssessedAtDesc(farmId);
        return list.stream().map(SoilAssessmentResp::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SoilAssessmentResp getSoilAssessmentDetails(Integer currentUserId, Integer assessmentId) {
        SoilAssessment entity = soilRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kết quả đánh giá thổ nhưỡng ID: " + assessmentId));
        accessGuard.requireBlockAccess(currentUserId, entity.getBlock().getId());
        return SoilAssessmentResp.fromEntity(entity);
    }

    @Override
    public void deleteSoilAssessment(Integer currentUserId, Integer assessmentId) {
        SoilAssessment entity = soilRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kết quả đánh giá thổ nhưỡng ID: " + assessmentId));
        accessGuard.requireFarmOwner(currentUserId, entity.getBlock().getZone().getFarm().getId());
        soilRepository.delete(entity);
    }
}

