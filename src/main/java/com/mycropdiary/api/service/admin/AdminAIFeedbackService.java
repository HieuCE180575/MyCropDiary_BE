package com.mycropdiary.api.service.admin;

import com.mycropdiary.api.dto.admin.AdminAIFeedbackResp;

import java.util.List;

public interface AdminAIFeedbackService {
    List<AdminAIFeedbackResp> getAllFeedbacks(String status, Integer rating);

    AdminAIFeedbackResp getFeedbackById(Integer feedbackId);

    AdminAIFeedbackResp replyFeedback(Integer adminUserId, Integer feedbackId, String replyText);

    AdminAIFeedbackResp updateFeedbackStatus(Integer adminUserId, Integer feedbackId, String status);
}

