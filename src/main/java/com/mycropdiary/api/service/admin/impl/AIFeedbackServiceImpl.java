package com.mycropdiary.api.service.admin.impl;

import com.mycropdiary.api.dto.admin.AIFeedbackDetailRespDTO;
import com.mycropdiary.api.entity.AIFeedback;
import com.mycropdiary.api.entity.AIFeedbackReply;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.mapper.AIFeedbackMapper;
import com.mycropdiary.api.repository.AIFeedbackRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.service.admin.AIFeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("adminAIFeedbackService")
@Transactional
public class AIFeedbackServiceImpl implements AIFeedbackService {

    private final AIFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final AIFeedbackMapper aiFeedbackMapper;

    @Autowired
    public AIFeedbackServiceImpl(
            AIFeedbackRepository feedbackRepository,
            UserRepository userRepository,
            AIFeedbackMapper aiFeedbackMapper) {
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
        this.aiFeedbackMapper = aiFeedbackMapper != null ? aiFeedbackMapper : new AIFeedbackMapper();
    }

    public AIFeedbackServiceImpl(AIFeedbackRepository feedbackRepository, UserRepository userRepository) {
        this(feedbackRepository, userRepository, new AIFeedbackMapper());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AIFeedbackDetailRespDTO> getAllFeedbacks(String status, Integer rating) {
        List<AIFeedback> list = feedbackRepository.findAllByOrderByCreatedAtDesc();
        return list.stream()
                .filter(f -> {
                    String s = f.getReplies() == null || f.getReplies().isEmpty() ? "OPEN" : "RESOLVED";
                    if (status != null && !status.isBlank() && !s.equalsIgnoreCase(status.trim())) {
                        return false;
                    }
                    if (rating != null && f.getRating() != null && !f.getRating().equals(rating)) {
                        return false;
                    }
                    return true;
                })
                .map(aiFeedbackMapper::toDetailResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AIFeedbackDetailRespDTO getFeedbackById(Integer feedbackId) {
        AIFeedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phản hồi AI ID: " + feedbackId));
        return aiFeedbackMapper.toDetailResponse(feedback);
    }

    @Override
    public AIFeedbackDetailRespDTO replyFeedback(Integer adminUserId, Integer feedbackId, String replyText) {
        AIFeedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phản hồi AI ID: " + feedbackId));

        User adminUser = adminUserId != null ? userRepository.findById(adminUserId).orElse(null) : null;

        AIFeedbackReply reply = new AIFeedbackReply(feedback, adminUser, replyText.trim());
        feedback.getReplies().add(reply);
        AIFeedback saved = feedbackRepository.save(feedback);
        return aiFeedbackMapper.toDetailResponse(saved);
    }

    @Override
    public AIFeedbackDetailRespDTO updateFeedbackStatus(Integer adminUserId, Integer feedbackId, String status) {
        AIFeedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phản hồi AI ID: " + feedbackId));

        if ("RESOLVED".equalsIgnoreCase(status) && (feedback.getReplies() == null || feedback.getReplies().isEmpty())) {
            User adminUser = adminUserId != null ? userRepository.findById(adminUserId).orElse(null) : null;
            AIFeedbackReply autoReply = new AIFeedbackReply(
                    feedback,
                    adminUser,
                    "Ban quản trị đã ghi nhận và xử lý góp ý cải tiến câu trả lời AI của bạn."
            );
            feedback.getReplies().add(autoReply);
            feedback = feedbackRepository.save(feedback);
        }
        return aiFeedbackMapper.toDetailResponse(feedback);
    }
}
