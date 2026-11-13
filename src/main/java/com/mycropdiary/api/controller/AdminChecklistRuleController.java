package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.entity.cultivation.ChecklistRule;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.cultivation.ChecklistRuleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/checklist-rules")
@Tag(name = "03. Quản trị - Quy tắc bảng kiểm (Admin)", description = "APIs quản lý bộ quy tắc bảng kiểm tra đánh giá chất lượng hồ sơ")
@SecurityRequirement(name = "BearerAuthentication")
@PreAuthorize("hasRole('ADMIN')")
public class AdminChecklistRuleController {

    private final ChecklistRuleRepository checklistRuleRepository;
    private final FarmRepository farmRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public AdminChecklistRuleController(ChecklistRuleRepository checklistRuleRepository, FarmRepository farmRepository) {
        this.checklistRuleRepository = checklistRuleRepository;
        this.farmRepository = farmRepository;
    }

    public AdminChecklistRuleController(ChecklistRuleRepository checklistRuleRepository) {
        this(checklistRuleRepository, null);
    }

    @Operation(summary = "Xem danh sách quy tắc bảng kiểm (Admin)", description = "Lấy toàn bộ danh sách quy tắc kiểm tra đánh giá nhật ký và hồ sơ VietGAP")
    @GetMapping
    public ApiResp<List<AdminChecklistRuleResp>> getChecklistRules() {
        List<ChecklistRule> rules = checklistRuleRepository.findAll();
        List<AdminChecklistRuleResp> list = rules.stream().map(r -> new AdminChecklistRuleResp(
                r.getId(),
                r.getRuleName(),
                r.getDescription(),
                r.getCategory(),
                r.getIsActive(),
                r.getCreatedAt()
        )).toList();
        return ApiResp.ok("Danh sách quy tắc bảng kiểm", list);
    }

    @Operation(summary = "Thêm mới quy tắc bảng kiểm (Admin)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<AdminChecklistRuleResp> createChecklistRule(@RequestBody AdminChecklistRuleReq req) {
        ChecklistRule rule = new ChecklistRule();
        if (farmRepository != null) {
            farmRepository.findAll().stream().findFirst().ifPresent(rule::setFarm);
        }
        rule.setRuleName(req.ruleName() != null ? req.ruleName().trim() : "Quy tắc VietGAP");
        rule.setDescription(req.description());
        rule.setCategory(req.category() != null ? req.category().trim() : "ALL");
        rule.setIsActive(req.isActive() != null ? req.isActive() : true);
        ChecklistRule saved = checklistRuleRepository.save(rule);
        return ApiResp.ok("Tạo quy tắc bảng kiểm thành công", new AdminChecklistRuleResp(
                saved.getId(), saved.getRuleName(), saved.getDescription(), saved.getCategory(), saved.getIsActive(), saved.getCreatedAt()
        ));
    }

    @Operation(summary = "Cập nhật quy tắc bảng kiểm (Admin)")
    @PutMapping("/{id}")
    public ApiResp<AdminChecklistRuleResp> updateChecklistRule(@PathVariable Integer id, @RequestBody AdminChecklistRuleReq req) {
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
        return ApiResp.ok("Cập nhật quy tắc thành công", new AdminChecklistRuleResp(
                saved.getId(), saved.getRuleName(), saved.getDescription(), saved.getCategory(), saved.getIsActive(), saved.getCreatedAt()
        ));
    }

    public record AdminChecklistRuleResp(
            Integer ruleId,
            String ruleName,
            String description,
            String category,
            Boolean isActive,
            Instant createdAt
    ) {}

    public record AdminChecklistRuleReq(
            String ruleName,
            String description,
            String category,
            Boolean isActive
    ) {}
}

