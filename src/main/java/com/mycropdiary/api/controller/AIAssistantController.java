package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.ai.*;
import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.service.AIAssistantService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "12 & 13. Trợ lý AI & Lịch sử hội thoại (AI Assistant & Feedback)", description = "APIs tư vấn nông nghiệp AI VietGAP, quản lý phiên trò chuyện, tìm kiếm và phản hồi/báo cáo câu trả lời")
@SecurityRequirement(name = "BearerAuthentication")
public class AIAssistantController {

    private final AIAssistantService aiService;
    private final SecurityUtils securityUtils;

    public AIAssistantController(AIAssistantService aiService, SecurityUtils securityUtils) {
        this.aiService = aiService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Tạo phiên hội thoại mới", description = "Khởi tạo một phiên trò chuyện mới với Trợ lý AI.")
    @PostMapping("/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<AIConversationResp> createConversation(@Valid @RequestBody CreateAIConversationReq req) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        AIConversationResp resp = aiService.createConversation(currentUserId, req);
        return ApiResp.ok("Khởi tạo cuộc hội thoại thành công.", resp);
    }

    @Operation(summary = "Xem và tìm kiếm lịch sử các cuộc hội thoại (UC-12.2 & UC-12.3)", description = "Lấy danh sách các cuộc hội thoại của tôi và tìm kiếm theo từ khóa trong tiêu đề/nội dung.")
    @GetMapping("/conversations")
    public ApiResp<List<AIConversationResp>> getMyConversations(@RequestParam(required = false) String keyword) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        List<AIConversationResp> list = aiService.getMyConversations(currentUserId, keyword);
        return ApiResp.ok("Lấy danh sách cuộc hội thoại thành công.", list);
    }

    @Operation(summary = "Xem chi tiết tin nhắn trong cuộc hội thoại", description = "Lấy toàn bộ lịch sử tin nhắn của một cuộc hội thoại cụ thể.")
    @GetMapping("/conversations/{conversationId}")
    public ApiResp<AIConversationDetailResp> getConversationDetail(@PathVariable Integer conversationId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        AIConversationDetailResp detail = aiService.getConversationDetail(currentUserId, conversationId);
        return ApiResp.ok("Lấy chi tiết cuộc hội thoại thành công.", detail);
    }

    @Operation(summary = "Hỏi Trợ lý AI trong phiên hội thoại (UC-12.1: Ask AI Assistant)", description = "Gửi câu hỏi về quy trình kỹ thuật, sâu bệnh, bón phân VietGAP và nhận phản hồi tức thì.")
    @PostMapping("/conversations/{conversationId}/messages")
    public ApiResp<AIMessageResp> askInConversation(
            @PathVariable Integer conversationId,
            @Valid @RequestBody AskAIQuestionReq req) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        AskAIQuestionReq effectiveReq = new AskAIQuestionReq(req.question(), conversationId, req.farmId());
        AIMessageResp resp = aiService.askAIAssistant(currentUserId, effectiveReq);
        return ApiResp.ok("Nhận câu trả lời từ Trợ lý AI thành công.", resp);
    }

    @Operation(summary = "Hỏi nhanh Trợ lý AI (Tự động tạo/tiếp tục phiên)", description = "Gửi câu hỏi mà không cần tự quản lý ID phiên hội thoại.")
    @PostMapping("/ask")
    public ApiResp<AIMessageResp> askQuick(@Valid @RequestBody AskAIQuestionReq req) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        AIMessageResp resp = aiService.askAIAssistant(currentUserId, req);
        return ApiResp.ok("Nhận câu trả lời từ Trợ lý AI thành công.", resp);
    }

    @Operation(summary = "Báo cáo câu trả lời AI chưa chuẩn xác (UC-13.4: Report AI Response)", description = "Người dùng đánh giá sao và gửi lý do phản hồi/báo cáo câu trả lời của AI.")
    @PostMapping("/messages/{messageId}/report")
    public ApiResp<AIFeedbackResp> reportAIMessage(
            @PathVariable Integer messageId,
            @Valid @RequestBody ReportAIMessageReq req) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        AIFeedbackResp resp = aiService.reportAIMessage(currentUserId, messageId, req);
        return ApiResp.ok("Gửi phản hồi báo cáo thành công.", resp);
    }

    @Operation(summary = "Xem lịch sử các phản hồi/báo cáo của tôi (UC-13.3: View Feedback History)", description = "Xem lại danh sách các ý kiến phản hồi đã gửi và phản hồi giải quyết từ ban cố vấn.")
    @GetMapping("/feedback/my-history")
    public ApiResp<List<AIFeedbackResp>> getMyFeedbackHistory() {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        List<AIFeedbackResp> list = aiService.getMyFeedbackHistory(currentUserId);
        return ApiResp.ok("Lấy lịch sử phản hồi thành công.", list);
    }

    @Operation(summary = "Cập nhật phản hồi báo cáo AI (UC-13: Update Feedback)", description = "Chỉnh sửa số sao đánh giá và nội dung lý do phản hồi đã gửi.")
    @PutMapping("/feedback/{feedbackId}")
    public ApiResp<AIFeedbackResp> updateFeedback(
            @PathVariable Integer feedbackId,
            @Valid @RequestBody ReportAIMessageReq req) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        AIFeedbackResp resp = aiService.updateFeedback(currentUserId, feedbackId, req);
        return ApiResp.ok("Cập nhật phản hồi thành công.", resp);
    }

    @Operation(summary = "Xóa cuộc hội thoại AI (UC-12: Delete Conversation)", description = "Xóa phiên hội thoại và toàn bộ tin nhắn liên quan.")
    @DeleteMapping("/conversations/{conversationId}")
    public ApiResp<Void> deleteConversation(@PathVariable Integer conversationId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        aiService.deleteConversation(currentUserId, conversationId);
        return ApiResp.ok("Xóa cuộc hội thoại thành công.", null);
    }
}

