package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.entity.cultivation.ChecklistRule;
import com.mycropdiary.api.exception.ResourceNotFoundException;
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

    public AdminChecklistRuleController(ChecklistRuleRepository checklistRuleRepository) {
        this.checklistRuleRepository = checklistRuleRepository;
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
        rule.setRuleName(req.ruleName());
        rule.setDescription(req.description());
        rule.setCategory(req.category());
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
        rule.setRuleName(req.ruleName());
        rule.setDescription(req.description());
        rule.setCategory(req.category());
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

