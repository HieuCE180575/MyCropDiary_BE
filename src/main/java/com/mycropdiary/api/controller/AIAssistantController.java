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
    public ApiResp<AIConversationRespDTO> createConversation(@Valid @RequestBody CreateAIConversationReqDTO req) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        AIConversationRespDTO resp = aiService.createConversation(currentUserId, req);
        return ApiResp.ok("Khởi tạo cuộc hội thoại thành công.", resp);
    }

    @Operation(summary = "Xem và tìm kiếm lịch sử các cuộc hội thoại (UC-12.2 & UC-12.3)", description = "Lấy danh sách các cuộc hội thoại của tôi và tìm kiếm theo từ khóa trong tiêu đề/nội dung.")
    @GetMapping("/conversations")
    public ApiResp<List<AIConversationRespDTO>> getMyConversations(@RequestParam(required = false) String keyword) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        List<AIConversationRespDTO> list = aiService.getMyConversations(currentUserId, keyword);
        return ApiResp.ok("Lấy danh sách cuộc hội thoại thành công.", list);
    }

    @Operation(summary = "Xem chi tiết tin nhắn trong cuộc hội thoại", description = "Lấy toàn bộ lịch sử tin nhắn của một cuộc hội thoại cụ thể.")
    @GetMapping("/conversations/{conversationId}")
    public ApiResp<AIConversationDetailRespDTO> getConversationDetail(@PathVariable Integer conversationId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        AIConversationDetailRespDTO detail = aiService.getConversationDetail(currentUserId, conversationId);
        return ApiResp.ok("Lấy chi tiết cuộc hội thoại thành công.", detail);
    }

    @Operation(summary = "Hỏi Trợ lý AI trong phiên hội thoại (UC-12.1: Ask AI Assistant)", description = "Gửi câu hỏi về quy trình kỹ thuật, sâu bệnh, bón phân VietGAP và nhận phản hồi tức thì.")
    @PostMapping("/conversations/{conversationId}/messages")
    public ApiResp<AIMessageRespDTO> askInConversation(
            @PathVariable Integer conversationId,
            @Valid @RequestBody AskAIQuestionReqDTO req) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        AskAIQuestionReqDTO effectiveReq = new AskAIQuestionReqDTO(req.question(), conversationId, req.farmId());
        AIMessageRespDTO resp = aiService.askAIAssistant(currentUserId, effectiveReq);
        return ApiResp.ok("Nhận câu trả lời từ Trợ lý AI thành công.", resp);
    }

    @Operation(summary = "Hỏi nhanh Trợ lý AI (Tự động tạo/tiếp tục phiên)", description = "Gửi câu hỏi mà không cần tự quản lý ID phiên hội thoại.")
    @PostMapping("/ask")
    public ApiResp<AIMessageRespDTO> askQuick(@Valid @RequestBody AskAIQuestionReqDTO req) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        AIMessageRespDTO resp = aiService.askAIAssistant(currentUserId, req);
        return ApiResp.ok("Nhận câu trả lời từ Trợ lý AI thành công.", resp);
    }

    @Operation(summary = "Xóa cuộc hội thoại AI (UC-12: Delete Conversation)", description = "Xóa phiên hội thoại và toàn bộ tin nhắn liên quan.")
    @DeleteMapping("/conversations/{conversationId}")
    public ApiResp<Void> deleteConversation(@PathVariable Integer conversationId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        aiService.deleteConversation(currentUserId, conversationId);
        return ApiResp.ok("Xóa cuộc hội thoại thành công.", null);
    }

    @Operation(summary = "Bóc tách hoạt động canh tác từ giọng nói / văn bản tự nhiên", description = "AI phân tích câu nói của người dùng và trả về các trường dữ liệu để tự động điền form.")
    @PostMapping("/parse-voice-activity")
    public ApiResp<FarmingDraftRespDTO> parseVoiceActivity(@Valid @RequestBody GenerateDraftReqDTO req) {
        FarmingDraftRespDTO resp = aiService.parseVoiceActivity(req);
        return ApiResp.ok("Bóc tách thông tin giọng nói thành công.", resp);
    }
}

