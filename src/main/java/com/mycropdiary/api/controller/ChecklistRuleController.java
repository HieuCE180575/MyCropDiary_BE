package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.admin.ChecklistRuleRespDTO;
import com.mycropdiary.api.dto.admin.CreateChecklistRuleReqDTO;
import com.mycropdiary.api.dto.admin.UpdateChecklistRuleReqDTO;
import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.service.admin.ChecklistRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/checklist-rules")
@Tag(name = "03. Quản trị - Quy tắc bảng kiểm (Admin)", description = "APIs quản lý bộ quy tắc bảng kiểm tra đánh giá chất lượng hồ sơ")
@SecurityRequirement(name = "BearerAuthentication")
@PreAuthorize("hasRole('ADMIN')")
public class ChecklistRuleController {

    private final ChecklistRuleService checklistRuleService;

    public ChecklistRuleController(ChecklistRuleService checklistRuleService) {
        this.checklistRuleService = checklistRuleService;
    }

    @Operation(summary = "Xem danh sách quy tắc bảng kiểm (Admin)", description = "Lấy toàn bộ danh sách quy tắc kiểm tra đánh giá nhật ký và hồ sơ VietGAP")
    @GetMapping
    public ApiResp<List<ChecklistRuleRespDTO>> getChecklistRules() {
        return ApiResp.ok("Danh sách quy tắc bảng kiểm", checklistRuleService.getChecklistRules());
    }

    @Operation(summary = "Thêm mới quy tắc bảng kiểm (Admin)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<ChecklistRuleRespDTO> createChecklistRule(@RequestBody CreateChecklistRuleReqDTO req) {
        return ApiResp.ok("Tạo quy tắc bảng kiểm thành công", checklistRuleService.createChecklistRule(req));
    }

    @Operation(summary = "Cập nhật quy tắc bảng kiểm (Admin)")
    @PutMapping("/{id}")
    public ApiResp<ChecklistRuleRespDTO> updateChecklistRule(@PathVariable Integer id, @RequestBody UpdateChecklistRuleReqDTO req) {
        return ApiResp.ok("Cập nhật quy tắc thành công", checklistRuleService.updateChecklistRule(id, req));
    }
}
