package com.mycropdiary.api.service.admin;

import com.mycropdiary.api.dto.admin.AIFeedbackDetailRespDTO;

import java.util.List;

public interface AIFeedbackService {
    List<AIFeedbackDetailRespDTO> getAllFeedbacks(String status, Integer rating);

    AIFeedbackDetailRespDTO getFeedbackById(Integer feedbackId);

    AIFeedbackDetailRespDTO replyFeedback(Integer adminUserId, Integer feedbackId, String replyText);

    AIFeedbackDetailRespDTO updateFeedbackStatus(Integer adminUserId, Integer feedbackId, String status);
}
