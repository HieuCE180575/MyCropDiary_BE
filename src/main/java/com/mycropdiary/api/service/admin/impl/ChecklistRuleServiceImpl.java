package com.mycropdiary.api.service.admin.impl;

import com.mycropdiary.api.dto.admin.ChecklistRuleRespDTO;
import com.mycropdiary.api.dto.admin.CreateChecklistRuleReqDTO;
import com.mycropdiary.api.dto.admin.UpdateChecklistRuleReqDTO;
import com.mycropdiary.api.entity.cultivation.ChecklistRule;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.mapper.ChecklistRuleMapper;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.cultivation.ChecklistRuleRepository;
import com.mycropdiary.api.service.admin.ChecklistRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ChecklistRuleServiceImpl implements ChecklistRuleService {

    private final ChecklistRuleRepository checklistRuleRepository;
    private final ChecklistRuleMapper checklistRuleMapper;

    @Autowired
    public ChecklistRuleServiceImpl(
            ChecklistRuleRepository checklistRuleRepository,
            FarmRepository farmRepository,
            ChecklistRuleMapper checklistRuleMapper) {
        this.checklistRuleRepository = checklistRuleRepository;
        this.checklistRuleMapper = checklistRuleMapper != null ? checklistRuleMapper : new ChecklistRuleMapper();
    }

    public ChecklistRuleServiceImpl(ChecklistRuleRepository checklistRuleRepository) {
        this(checklistRuleRepository, null, new ChecklistRuleMapper());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChecklistRuleRespDTO> getChecklistRules() {
        // Đồng bộ 100% bộ tiêu chí Master VietGAP (farm_id IS NULL) giữa Admin và Owner
        List<ChecklistRule> masterRules = checklistRuleRepository.findAllSystemMasterRules();
        if (masterRules == null || masterRules.isEmpty()) {
            masterRules = checklistRuleRepository.findAll();
        }
        return checklistRuleMapper.toResponseList(masterRules);
    }

    @Override
    public ChecklistRuleRespDTO createChecklistRule(CreateChecklistRuleReqDTO req) {
        ChecklistRule rule = new ChecklistRule();
        // Quy tắc do Admin tạo là tiêu chí Master của hệ thống (farm = null) để mọi trang trại (Owner) đều đồng bộ
        rule.setFarm(null);

        String code = req.ruleCode() != null && !req.ruleCode().isBlank()
                ? req.ruleCode().trim().toUpperCase()
                : String.format("VG-TC-%02d", checklistRuleRepository.count() + 1);
        rule.setRuleCode(code);
        rule.setRuleName(req.ruleName() != null && !req.ruleName().isBlank() ? req.ruleName().trim() : "Quy tắc VietGAP");
        rule.setDescription(req.description() != null ? req.description().trim() : "");
        rule.setCategory(req.category() != null && !req.category().isBlank() ? req.category().trim() : "RECORDS_AND_TRAINING");
        rule.setRuleLevel(normalizeRuleLevel(req.ruleLevel()));
        rule.setTargetCropType(req.targetCropType() != null && !req.targetCropType().isBlank() ? req.targetCropType().trim().toUpperCase() : "ALL");
        rule.setIsActive(req.isActive() != null ? req.isActive() : true);

        ChecklistRule saved = checklistRuleRepository.save(rule);
        return checklistRuleMapper.toResponse(saved);
    }

    @Override
    public ChecklistRuleRespDTO updateChecklistRule(Integer id, UpdateChecklistRuleReqDTO req) {
        ChecklistRule rule = checklistRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quy tắc ID: " + id));
        if (req.ruleCode() != null && !req.ruleCode().isBlank()) {
            rule.setRuleCode(req.ruleCode().trim().toUpperCase());
        }
        if (req.ruleName() != null && !req.ruleName().isBlank()) {
            rule.setRuleName(req.ruleName().trim());
        }
        if (req.description() != null) {
            rule.setDescription(req.description().trim());
        }
        if (req.category() != null && !req.category().isBlank()) {
            rule.setCategory(req.category().trim());
        }
        if (req.ruleLevel() != null && !req.ruleLevel().isBlank()) {
            rule.setRuleLevel(normalizeRuleLevel(req.ruleLevel()));
        }
        if (req.targetCropType() != null && !req.targetCropType().isBlank()) {
            rule.setTargetCropType(req.targetCropType().trim().toUpperCase());
        }
        if (req.isActive() != null) {
            rule.setIsActive(req.isActive());
        }
        ChecklistRule saved = checklistRuleRepository.save(rule);
        return checklistRuleMapper.toResponse(saved);
    }

    private String normalizeRuleLevel(String level) {
        if (level == null || level.isBlank()) {
            return "MUST";
        }
        String normalized = level.trim().toUpperCase();
        if ("REQUIRED".equals(normalized) || "MUST".equals(normalized)) {
            return "MUST";
        }
        if ("RECOMMENDED".equals(normalized)) {
            return "RECOMMENDED";
        }
        if ("NOT_APPLICABLE".equals(normalized)) {
            return "NOT_APPLICABLE";
        }
        return "MUST";
    }
}
