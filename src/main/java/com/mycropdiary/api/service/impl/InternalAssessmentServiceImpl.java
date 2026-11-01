package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.vietgap.ConductInternalAssessmentReq;
import com.mycropdiary.api.dto.vietgap.InternalAssessmentItemReq;
import com.mycropdiary.api.dto.vietgap.InternalAssessmentItemResp;
import com.mycropdiary.api.dto.vietgap.InternalAssessmentResp;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.Zone;
import com.mycropdiary.api.entity.cultivation.AssessmentCriterion;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import com.mycropdiary.api.entity.cultivation.InternalAssessment;
import com.mycropdiary.api.entity.cultivation.InternalAssessmentItem;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.ZoneRepository;
import com.mycropdiary.api.repository.cultivation.AssessmentCriterionRepository;
import com.mycropdiary.api.repository.cultivation.CropSeasonRepository;
import com.mycropdiary.api.repository.cultivation.InternalAssessmentItemRepository;
import com.mycropdiary.api.repository.cultivation.InternalAssessmentRepository;
import com.mycropdiary.api.service.cultivation.InternalAssessmentService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class InternalAssessmentServiceImpl implements InternalAssessmentService {

    private final InternalAssessmentRepository assessmentRepository;
    private final InternalAssessmentItemRepository itemRepository;
    private final AssessmentCriterionRepository criterionRepository;
    private final FarmRepository farmRepository;
    private final CropSeasonRepository cropSeasonRepository;
    private final ZoneRepository zoneRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final FarmAccessGuard accessGuard;

    public InternalAssessmentServiceImpl(InternalAssessmentRepository assessmentRepository,
                                         InternalAssessmentItemRepository itemRepository,
                                         AssessmentCriterionRepository criterionRepository,
                                         FarmRepository farmRepository,
                                         CropSeasonRepository cropSeasonRepository,
                                         ZoneRepository zoneRepository,
                                         FarmMemberRepository farmMemberRepository,
                                         FarmAccessGuard accessGuard) {
        this.assessmentRepository = assessmentRepository;
        this.itemRepository = itemRepository;
        this.criterionRepository = criterionRepository;
        this.farmRepository = farmRepository;
        this.cropSeasonRepository = cropSeasonRepository;
        this.zoneRepository = zoneRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.accessGuard = accessGuard;
    }

    @Override
    public InternalAssessmentResp conductInternalAssessment(Integer currentUserId, Integer farmId, ConductInternalAssessmentReq request) {
        accessGuard.requireFarmAccess(currentUserId, farmId);

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trang trại ID: " + farmId));

        CropSeason season = null;
        if (request.cropSeasonId() != null) {
            season = cropSeasonRepository.findById(request.cropSeasonId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mùa vụ ID: " + request.cropSeasonId()));
            if (!season.getFarm().getId().equals(farmId)) {
                throw new BadRequestException("Mùa vụ không thuộc trang trại này");
            }
        }

        Zone zone = null;
        if (request.zoneId() != null) {
            zone = zoneRepository.findById(request.zoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phân khu ID: " + request.zoneId()));
            if (!zone.getFarm().getId().equals(farmId)) {
                throw new BadRequestException("Phân khu không thuộc trang trại này");
            }
        }

        FarmMember assessor = farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, currentUserId, "active")
                .orElse(null);

        InternalAssessment assessment = new InternalAssessment(
                farm,
                season,
                zone,
                assessor,
                request.assessmentTitle().trim(),
                request.assessedAt() != null ? request.assessedAt() : Instant.now(),
                request.notes() != null ? request.notes().trim() : null,
                request.evidenceImageUrl() != null ? request.evidenceImageUrl().trim() : null
        );

        InternalAssessment savedAssessment = assessmentRepository.save(assessment);

        List<InternalAssessmentItemResp> itemResps = new ArrayList<>();
        for (InternalAssessmentItemReq itemReq : request.items()) {
            AssessmentCriterion criterion = criterionRepository.findById(itemReq.assessmentCriterionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tiêu chí đánh giá ID: " + itemReq.assessmentCriterionId()));

            if (!criterion.getFarm().getId().equals(farmId)) {
                throw new BadRequestException("Tiêu chí '" + criterion.getCriterionName() + "' không thuộc trang trại này");
            }

            if (itemReq.score().compareTo(criterion.getMaxScore()) > 0) {
                throw new BadRequestException("Điểm số (" + itemReq.score() + ") cho tiêu chí '"
                        + criterion.getCriterionName() + "' vượt quá điểm tối đa (" + criterion.getMaxScore() + ")");
            }

            InternalAssessmentItem item = new InternalAssessmentItem(
                    savedAssessment,
                    criterion,
                    itemReq.score(),
                    itemReq.resultNotes() != null ? itemReq.resultNotes().trim() : null,
                    itemReq.evidenceImageUrl() != null ? itemReq.evidenceImageUrl().trim() : null
            );
            InternalAssessmentItem savedItem = itemRepository.save(item);
            itemResps.add(InternalAssessmentItemResp.fromEntity(savedItem));
        }

        return InternalAssessmentResp.fromEntity(savedAssessment, itemResps);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InternalAssessmentResp> getInternalAssessmentHistory(Integer currentUserId, Integer farmId, Integer cropSeasonId, Integer zoneId) {
        accessGuard.requireFarmAccess(currentUserId, farmId);

        List<InternalAssessment> assessments = assessmentRepository.findAssessmentsByFarm(farmId, cropSeasonId, zoneId);

        return assessments.stream().map(assessment -> {
            List<InternalAssessmentItem> items = itemRepository.findByInternalAssessmentIdWithCriterion(assessment.getId());
            List<InternalAssessmentItemResp> itemResps = items.stream().map(InternalAssessmentItemResp::fromEntity).toList();
            return InternalAssessmentResp.fromEntity(assessment, itemResps);
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InternalAssessmentResp getInternalAssessmentDetails(Integer currentUserId, Integer assessmentId) {
        InternalAssessment assessment = assessmentRepository.findByIdWithDetails(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đợt đánh giá nội bộ ID: " + assessmentId));

        accessGuard.requireFarmAccess(currentUserId, assessment.getFarm().getId());

        List<InternalAssessmentItem> items = itemRepository.findByInternalAssessmentIdWithCriterion(assessmentId);
        List<InternalAssessmentItemResp> itemResps = items.stream().map(InternalAssessmentItemResp::fromEntity).toList();

        return InternalAssessmentResp.fromEntity(assessment, itemResps);
    }
}

