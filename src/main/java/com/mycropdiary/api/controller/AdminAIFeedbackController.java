package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.admin.AdminAIFeedbackResp;
import com.mycropdiary.api.dto.admin.AdminReplyFeedbackReq;
import com.mycropdiary.api.dto.admin.AdminUpdateFeedbackStatusReq;
import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.service.admin.AdminAIFeedbackService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/ai-feedback")
@Tag(name = "03. Quản trị - Phản hồi AI (Admin)", description = "APIs dành cho Quản trị viên quản lý, xem chi tiết và phản hồi góp ý câu trả lời AI")
@SecurityRequirement(name = "BearerAuthentication")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAIFeedbackController {

    private final AdminAIFeedbackService adminAIFeedbackService;
    private final SecurityUtils securityUtils;

    public AdminAIFeedbackController(AdminAIFeedbackService adminAIFeedbackService, SecurityUtils securityUtils) {
        this.adminAIFeedbackService = adminAIFeedbackService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Xem danh sách phản hồi AI", description = "Admin xem toàn bộ phản hồi từ người dùng về câu trả lời của trợ lý AI")
    @GetMapping
    public ApiResp<List<AdminAIFeedbackResp>> getFeedbacks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer rating) {
        List<AdminAIFeedbackResp> list = adminAIFeedbackService.getAllFeedbacks(status, rating);
        return ApiResp.ok("Danh sách phản hồi AI", list);
    }

    @Operation(summary = "Xem chi tiết phản hồi AI", description = "Admin xem chi tiết nội dung phản hồi, ngữ cảnh câu hỏi và các trao đổi giải quyết")
    @GetMapping("/{id}")
    public ApiResp<AdminAIFeedbackResp> getFeedbackById(@PathVariable Integer id) {
        AdminAIFeedbackResp resp = adminAIFeedbackService.getFeedbackById(id);
        return ApiResp.ok("Chi tiết phản hồi AI", resp);
    }

    @Operation(summary = "Trả lời phản hồi AI", description = "Admin gửi phản hồi giải thích hoặc hướng dẫn lại cho người dùng")
    @PostMapping("/{id}/reply")
    public ApiResp<AdminAIFeedbackResp> replyFeedback(
            @PathVariable Integer id,
            @Valid @RequestBody AdminReplyFeedbackReq request) {
        Integer adminUserId = securityUtils.getCurrentUserId();
        AdminAIFeedbackResp resp = adminAIFeedbackService.replyFeedback(adminUserId, id, request.replyText());
        return ApiResp.ok("Phản hồi ý kiến thành công", resp);
    }

    @Operation(summary = "Cập nhật trạng thái phản hồi AI", description = "Admin cập nhật trạng thái xử lý: OPEN (Mới) hoặc RESOLVED (Đã giải quyết)")
    @PatchMapping("/{id}/status")
    public ApiResp<AdminAIFeedbackResp> updateStatus(
            @PathVariable Integer id,
            @Valid @RequestBody AdminUpdateFeedbackStatusReq request) {
        Integer adminUserId = securityUtils.getCurrentUserId();
        AdminAIFeedbackResp resp = adminAIFeedbackService.updateFeedbackStatus(adminUserId, id, request.status());
        return ApiResp.ok("Cập nhật trạng thái phản hồi thành công", resp);
    }
}

