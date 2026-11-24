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
    private final FarmRepository farmRepository;
    private final ChecklistRuleMapper checklistRuleMapper;

    @Autowired
    public ChecklistRuleServiceImpl(
            ChecklistRuleRepository checklistRuleRepository,
            FarmRepository farmRepository,
            ChecklistRuleMapper checklistRuleMapper) {
        this.checklistRuleRepository = checklistRuleRepository;
        this.farmRepository = farmRepository;
        this.checklistRuleMapper = checklistRuleMapper != null ? checklistRuleMapper : new ChecklistRuleMapper();
    }

    public ChecklistRuleServiceImpl(ChecklistRuleRepository checklistRuleRepository) {
        this(checklistRuleRepository, null, new ChecklistRuleMapper());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChecklistRuleRespDTO> getChecklistRules() {
        return checklistRuleMapper.toResponseList(checklistRuleRepository.findAll());
    }

    @Override
    public ChecklistRuleRespDTO createChecklistRule(CreateChecklistRuleReqDTO req) {
        ChecklistRule rule = new ChecklistRule();
        if (farmRepository != null) {
            farmRepository.findAll().stream().findFirst().ifPresent(rule::setFarm);
        }
        rule.setRuleName(req.ruleName() != null ? req.ruleName().trim() : "Quy tắc VietGAP");
        rule.setDescription(req.description());
        rule.setCategory(req.category() != null ? req.category().trim() : "ALL");
        rule.setIsActive(req.isActive() != null ? req.isActive() : true);
        ChecklistRule saved = checklistRuleRepository.save(rule);
        return checklistRuleMapper.toResponse(saved);
    }

    @Override
    public ChecklistRuleRespDTO updateChecklistRule(Integer id, UpdateChecklistRuleReqDTO req) {
        ChecklistRule rule = checklistRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quy tắc ID: " + id));
        if (req.ruleName() != null && !req.ruleName().isBlank()) {
            rule.setRuleName(req.ruleName().trim());
        }
        if (req.description() != null) {
            rule.setDescription(req.description());
        }
        if (req.category() != null && !req.category().isBlank()) {
            rule.setCategory(req.category().trim());
        }
        if (req.isActive() != null) {
            rule.setIsActive(req.isActive());
        }
        ChecklistRule saved = checklistRuleRepository.save(rule);
        return checklistRuleMapper.toResponse(saved);
    }
}
