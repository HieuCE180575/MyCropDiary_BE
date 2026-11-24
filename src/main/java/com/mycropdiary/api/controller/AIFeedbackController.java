package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.ai.AIFeedbackRespDTO;
import com.mycropdiary.api.dto.ai.ReportAIMessageReqDTO;
import com.mycropdiary.api.dto.ai.UpdateAIFeedbackReqDTO;
import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.service.AIFeedbackService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("userAIFeedbackController")
@RequestMapping("/api/v1/ai")
@Tag(name = "13. Phản hồi câu trả lời AI (AI Feedback)", description = "APIs báo cáo câu trả lời AI, xem lịch sử và cập nhật phản hồi của người dùng")
@SecurityRequirement(name = "BearerAuthentication")
public class AIFeedbackController {

    private final AIFeedbackService aiFeedbackService;
    private final SecurityUtils securityUtils;

    public AIFeedbackController(AIFeedbackService aiFeedbackService, SecurityUtils securityUtils) {
        this.aiFeedbackService = aiFeedbackService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Báo cáo câu trả lời AI chưa chuẩn xác (UC-13.4: Report AI Response)", description = "Người dùng đánh giá sao và gửi lý do phản hồi/báo cáo câu trả lời của AI.")
    @PostMapping("/messages/{messageId}/report")
    public ApiResp<AIFeedbackRespDTO> reportAIMessage(
            @PathVariable Integer messageId,
            @Valid @RequestBody ReportAIMessageReqDTO req) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        AIFeedbackRespDTO resp = aiFeedbackService.reportAIMessage(currentUserId, messageId, req);
        return ApiResp.ok("Gửi phản hồi báo cáo thành công.", resp);
    }

    @Operation(summary = "Xem lịch sử các phản hồi/báo cáo của tôi (UC-13.3: View Feedback History)", description = "Xem lại danh sách các ý kiến phản hồi đã gửi và phản hồi giải quyết từ ban cố vấn.")
    @GetMapping("/feedback/my-history")
    public ApiResp<List<AIFeedbackRespDTO>> getMyFeedbackHistory() {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        List<AIFeedbackRespDTO> list = aiFeedbackService.getMyFeedbackHistory(currentUserId);
        return ApiResp.ok("Lấy lịch sử phản hồi thành công.", list);
    }

    @Operation(summary = "Cập nhật phản hồi báo cáo AI (UC-13: Update Feedback)", description = "Chỉnh sửa số sao đánh giá và nội dung lý do phản hồi đã gửi.")
    @PutMapping("/feedback/{feedbackId}")
    public ApiResp<AIFeedbackRespDTO> updateFeedback(
            @PathVariable Integer feedbackId,
            @Valid @RequestBody UpdateAIFeedbackReqDTO req) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        AIFeedbackRespDTO resp = aiFeedbackService.updateFeedback(currentUserId, feedbackId, req);
        return ApiResp.ok("Cập nhật phản hồi thành công.", resp);
    }
}
