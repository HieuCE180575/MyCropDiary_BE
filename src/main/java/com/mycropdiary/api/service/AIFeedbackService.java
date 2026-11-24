package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.ai.AIFeedbackRespDTO;
import com.mycropdiary.api.dto.ai.ReportAIMessageReqDTO;
import com.mycropdiary.api.dto.ai.UpdateAIFeedbackReqDTO;

import java.util.List;

public interface AIFeedbackService {
    AIFeedbackRespDTO reportAIMessage(Integer userId, Integer messageId, ReportAIMessageReqDTO req);
    List<AIFeedbackRespDTO> getMyFeedbackHistory(Integer userId);
    AIFeedbackRespDTO updateFeedback(Integer userId, Integer feedbackId, UpdateAIFeedbackReqDTO req);
}
