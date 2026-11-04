package com.mycropdiary.api.service.admin.impl;

import com.mycropdiary.api.dto.admin.AdminAIFeedbackResp;
import com.mycropdiary.api.entity.AIFeedback;
import com.mycropdiary.api.entity.AIFeedbackReply;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.AIFeedbackRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.service.admin.AdminAIFeedbackService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AdminAIFeedbackServiceImpl implements AdminAIFeedbackService {

    private final AIFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    public AdminAIFeedbackServiceImpl(AIFeedbackRepository feedbackRepository, UserRepository userRepository) {
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminAIFeedbackResp> getAllFeedbacks(String status, Integer rating) {
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
                .map(this::mapToResp)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAIFeedbackResp getFeedbackById(Integer feedbackId) {
        AIFeedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phản hồi AI ID: " + feedbackId));
        return mapToResp(feedback);
    }

    @Override
    public AdminAIFeedbackResp replyFeedback(Integer adminUserId, Integer feedbackId, String replyText) {
        AIFeedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phản hồi AI ID: " + feedbackId));

        User adminUser = null;
        if (adminUserId != null) {
            adminUser = userRepository.findById(adminUserId).orElse(null);
        }

        AIFeedbackReply reply = new AIFeedbackReply(feedback, adminUser, replyText.trim());
        feedback.getReplies().add(reply);
        AIFeedback saved = feedbackRepository.save(feedback);
        return mapToResp(saved);
    }

    @Override
    public AdminAIFeedbackResp updateFeedbackStatus(Integer adminUserId, Integer feedbackId, String status) {
        AIFeedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phản hồi AI ID: " + feedbackId));

        // If status requested is RESOLVED and no reply, add system acknowledgment
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
        return mapToResp(feedback);
    }

    private AdminAIFeedbackResp mapToResp(AIFeedback f) {
        String msgText = f.getMessage() != null ? f.getMessage().getMessageText() : null;
        String userQuestion = null;
        if (f.getMessage() != null && f.getMessage().getConversation() != null) {
            userQuestion = f.getMessage().getConversation().getConversationTitle();
        }

        String senderName = f.getUser() != null ? f.getUser().getFullName() : "Nông dân";
        String senderEmail = f.getUser() != null ? f.getUser().getEmail() : null;
        Integer userId = f.getUser() != null ? f.getUser().getId() : null;

        String status = (f.getReplies() == null || f.getReplies().isEmpty()) ? "OPEN" : "RESOLVED";

        List<AdminAIFeedbackResp.ReplyItem> replies = f.getReplies() == null ? List.of() :
                f.getReplies().stream().map(r -> new AdminAIFeedbackResp.ReplyItem(
                        r.getId(),
                        r.getReplyText(),
                        r.getUser() != null ? r.getUser().getFullName() : "Quản trị viên",
                        r.getCreatedAt()
                )).toList();

        return new AdminAIFeedbackResp(
                f.getId(),
                f.getMessage() != null ? f.getMessage().getId() : null,
                msgText,
                userQuestion,
                userId,
                senderName,
                senderEmail,
                f.getRating(),
                f.getFeedbackText(),
                status,
                f.getCreatedAt(),
                replies
        );
    }
}

