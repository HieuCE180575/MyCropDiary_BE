package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.compliance.HazardousWasteReqDTO;
import com.mycropdiary.api.dto.compliance.HazardousWasteRespDTO;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.compliance.HazardousWasteLog;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.BusinessRuleException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.mapper.HazardousWasteMapper;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.compliance.HazardousWasteLogRepository;
import com.mycropdiary.api.service.compliance.HazardousWasteService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Service nghiệp vụ cho UC-47: Manage Waste Records (Mục 4.8 VietGAP, BR-98).
 * Dùng chung cho cả Staff và Owner.
 */
@Service
@Transactional
public class HazardousWasteServiceImpl implements HazardousWasteService {

    private static final Set<String> VALID_WASTE_TYPES = Set.of("pesticide_bottle", "fertilizer_pack", "medical", "other");
    private static final Set<String> VALID_DISPOSAL_METHODS = Set.of("transfer_contractor", "incinerate_farm", "store_holding");

    private final FarmRepository farmRepository;
    private final HazardousWasteLogRepository wasteLogRepository;
    private final FarmAccessGuard accessGuard;
    private final HazardousWasteMapper mapper;

    public HazardousWasteServiceImpl(FarmRepository farmRepository,
                                     HazardousWasteLogRepository wasteLogRepository,
                                     FarmAccessGuard accessGuard,
                                     HazardousWasteMapper mapper) {
        this.farmRepository = farmRepository;
        this.wasteLogRepository = wasteLogRepository;
        this.accessGuard = accessGuard;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HazardousWasteRespDTO> getWasteLogs(Integer currentUserId, Integer farmId) {
        accessGuard.requireFarmAccess(currentUserId, farmId);
        return wasteLogRepository.findByFarmIdOrderByCollectionDateDescIdDesc(farmId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public HazardousWasteRespDTO createWasteLog(Integer currentUserId, Integer farmId, HazardousWasteReqDTO request) {
        accessGuard.requireFarmAccess(currentUserId, farmId);
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trang trại ID=" + farmId));

        validateRequest(request);

        HazardousWasteLog log = new HazardousWasteLog();
        log.setFarm(farm);
        applyFields(log, request);
        return mapper.toResponse(wasteLogRepository.save(log));
    }

    @Override
    public HazardousWasteRespDTO updateWasteLog(Integer currentUserId, Integer wasteLogId, HazardousWasteReqDTO request) {
        HazardousWasteLog log = wasteLogRepository.findById(wasteLogId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhật ký rác thải ID=" + wasteLogId));
        accessGuard.requireFarmAccess(currentUserId, log.getFarm().getId());

        validateRequest(request);
        applyFields(log, request);
        return mapper.toResponse(wasteLogRepository.save(log));
    }

    @Override
    public void deleteWasteLog(Integer currentUserId, Integer wasteLogId) {
        HazardousWasteLog log = wasteLogRepository.findById(wasteLogId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhật ký rác thải ID=" + wasteLogId));
        accessGuard.requireFarmAccess(currentUserId, log.getFarm().getId());
        wasteLogRepository.delete(log);
    }

    private void validateRequest(HazardousWasteReqDTO request) {
        String wasteType = request.wasteType().trim().toLowerCase();
        if (!VALID_WASTE_TYPES.contains(wasteType)) {
            throw new BadRequestException("Loại rác thải không hợp lệ.");
        }
        String disposalMethod = request.disposalMethod().trim().toLowerCase();
        if (!VALID_DISPOSAL_METHODS.contains(disposalMethod)) {
            throw new BadRequestException("Phương pháp xử lý rác thải không hợp lệ.");
        }
        // BR-98: Khi bàn giao đơn vị xử lý, bắt buộc ghi tên đơn vị và mã biên bản bàn giao
        if ("transfer_contractor".equals(disposalMethod)) {
            if (request.contractorName() == null || request.contractorName().isBlank()) {
                throw new BusinessRuleException("Vui lòng nhập tên đơn vị thu gom/xử lý rác thải nguy hại (BR-98).");
            }
            if (request.handoverReceiptNo() == null || request.handoverReceiptNo().isBlank()) {
                throw new BusinessRuleException("Vui lòng nhập số biên bản bàn giao rác thải nguy hại (BR-98).");
            }
        }
    }

    private void applyFields(HazardousWasteLog log, HazardousWasteReqDTO request) {
        log.setWasteType(request.wasteType().trim().toLowerCase());
        log.setQuantity(request.quantity());
        log.setUnit(request.unit().trim());
        log.setCollectionDate(request.collectionDate());
        log.setDisposalMethod(request.disposalMethod().trim().toLowerCase());
        log.setContractorName(request.contractorName() != null && !request.contractorName().isBlank() ? request.contractorName().trim() : null);
        log.setHandoverReceiptNo(request.handoverReceiptNo() != null && !request.handoverReceiptNo().isBlank() ? request.handoverReceiptNo().trim() : null);
        log.setCollectorName(request.collectorName().trim());
        log.setNotes(request.notes());
    }
}
