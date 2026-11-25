package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.compliance.CreateComplaintReqDTO;
import com.mycropdiary.api.dto.compliance.CustomerComplaintRespDTO;
import com.mycropdiary.api.dto.compliance.RecallProductReqDTO;
import com.mycropdiary.api.dto.compliance.ResolveComplaintReqDTO;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.compliance.CustomerComplaintLog;
import com.mycropdiary.api.entity.cultivation.HarvestRecord;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.BusinessRuleException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.mapper.CustomerComplaintMapper;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.compliance.CustomerComplaintLogRepository;
import com.mycropdiary.api.repository.cultivation.HarvestRecordRepository;
import com.mycropdiary.api.service.compliance.CustomerComplaintService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Service nghiệp vụ cho UC-48: Manage Customer Complaints & Product Recall (Mục 4.10 VietGAP, BR-100).
 * - Staff + Owner: Ghi nhận & Xem danh sách khiếu nại.
 * - Owner-only: UC-48.4 Resolve Complaint & UC-48.5 Record Product Recall.
 */
@Service
@Transactional
public class CustomerComplaintServiceImpl implements CustomerComplaintService {

    private static final Set<String> VALID_ISSUE_TYPES = Set.of("quality_defect", "chemical_residue_suspect", "packaging_damage", "other");
    private static final Set<String> VALID_SEVERITIES = Set.of("low", "medium", "critical");
    private static final Set<String> VALID_RESOLVE_STATUSES = Set.of("investigating", "resolved");

    private final FarmRepository farmRepository;
    private final HarvestRecordRepository harvestRecordRepository;
    private final CustomerComplaintLogRepository complaintRepository;
    private final FarmAccessGuard accessGuard;
    private final CustomerComplaintMapper mapper;

    public CustomerComplaintServiceImpl(FarmRepository farmRepository,
                                        HarvestRecordRepository harvestRecordRepository,
                                        CustomerComplaintLogRepository complaintRepository,
                                        FarmAccessGuard accessGuard,
                                        CustomerComplaintMapper mapper) {
        this.farmRepository = farmRepository;
        this.harvestRecordRepository = harvestRecordRepository;
        this.complaintRepository = complaintRepository;
        this.accessGuard = accessGuard;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerComplaintRespDTO> getComplaints(Integer currentUserId, Integer farmId) {
        accessGuard.requireFarmAccess(currentUserId, farmId);
        return complaintRepository.findByFarmIdOrderByReceivedDateDescIdDesc(farmId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public CustomerComplaintRespDTO createComplaint(Integer currentUserId, Integer farmId, CreateComplaintReqDTO request) {
        // Staff + Owner có thể ghi nhận khiếu nại mới
        accessGuard.requireFarmAccess(currentUserId, farmId);
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trang trại ID=" + farmId));

        String issueType = request.issueType().trim().toLowerCase();
        if (!VALID_ISSUE_TYPES.contains(issueType)) {
            throw new BadRequestException("Phân loại vấn đề khiếu nại không hợp lệ.");
        }
        String severity = request.severity().trim().toLowerCase();
        if (!VALID_SEVERITIES.contains(severity)) {
            throw new BadRequestException("Mức độ nghiêm trọng không hợp lệ (chấp nhận: low, medium, critical).");
        }

        HarvestRecord harvestRecord = null;
        String resolvedLotCode = request.lotCode() != null && !request.lotCode().isBlank() ? request.lotCode().trim() : null;

        if (request.harvestRecordId() != null) {
            harvestRecord = harvestRecordRepository.findById(request.harvestRecordId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lô thu hoạch ID=" + request.harvestRecordId()));
            Integer hrFarmId = harvestRecord.getFarm() != null ? harvestRecord.getFarm().getId()
                    : (harvestRecord.getCropSeason() != null && harvestRecord.getCropSeason().getFarm() != null
                    ? harvestRecord.getCropSeason().getFarm().getId() : null);
            if (!farmId.equals(hrFarmId)) {
                throw new BusinessRuleException("Lô thu hoạch được chọn không thuộc trang trại hiện tại.");
            }
            if (resolvedLotCode == null) {
                resolvedLotCode = harvestRecord.getHarvestLotCode();
            }
        }

        CustomerComplaintLog log = new CustomerComplaintLog();
        log.setFarm(farm);
        log.setHarvestRecord(harvestRecord);
        log.setLotCode(resolvedLotCode);
        log.setCustomerName(request.customerName().trim());
        log.setCustomerPhone(request.customerPhone() != null ? request.customerPhone().trim() : null);
        log.setReceivedDate(request.receivedDate());
        log.setIssueType(issueType);
        log.setIssueDescription(request.issueDescription().trim());
        log.setSeverity(severity);
        log.setStatus("received");
        log.setIsRecallRequired(false);

        return mapper.toResponse(complaintRepository.save(log));
    }

    @Override
    public CustomerComplaintRespDTO resolveComplaint(Integer currentUserId, Integer complaintId, ResolveComplaintReqDTO request) {
        // UC-48.4: Chỉ Owner được giải quyết / cập nhật phương án xử lý khiếu nại
        CustomerComplaintLog log = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ khiếu nại ID=" + complaintId));
        accessGuard.requireFarmOwner(currentUserId, log.getFarm().getId());

        String nextStatus = request.status().trim().toLowerCase();
        if (!VALID_RESOLVE_STATUSES.contains(nextStatus)) {
            throw new BadRequestException("Trạng thái xử lý không hợp lệ (chấp nhận: investigating, resolved).");
        }

        log.setStatus(nextStatus);
        log.setResolutionNotes(request.resolutionNotes().trim());
        return mapper.toResponse(complaintRepository.save(log));
    }

    @Override
    public CustomerComplaintRespDTO recordProductRecall(Integer currentUserId, Integer complaintId, RecallProductReqDTO request) {
        // UC-48.5: Chỉ Owner được phát lệnh thu hồi sản phẩm (BR-100)
        CustomerComplaintLog log = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ khiếu nại ID=" + complaintId));
        accessGuard.requireFarmOwner(currentUserId, log.getFarm().getId());

        // BR-100: Số lượng thu hồi không được vượt quá sản lượng thu hoạch của lô liên kết
        HarvestRecord hr = log.getHarvestRecord();
        if (hr != null && hr.getQuantity() != null && request.recalledQuantity().compareTo(hr.getQuantity()) > 0) {
            throw new BusinessRuleException(
                    "Số lượng thu hồi (" + request.recalledQuantity() + ") không được vượt quá sản lượng thu hoạch của lô "
                            + hr.getHarvestLotCode() + " (" + hr.getQuantity() + " " + hr.getUnit() + ") theo quy tắc BR-100.");
        }

        log.setIsRecallRequired(true);
        log.setRecalledQuantity(request.recalledQuantity());
        log.setStatus("recalled");
        log.setResolutionNotes(request.resolutionNotes().trim());

        return mapper.toResponse(complaintRepository.save(log));
    }

    @Override
    public void deleteComplaint(Integer currentUserId, Integer complaintId) {
        CustomerComplaintLog log = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ khiếu nại ID=" + complaintId));
        accessGuard.requireFarmOwner(currentUserId, log.getFarm().getId());
        complaintRepository.delete(log);
    }
}
