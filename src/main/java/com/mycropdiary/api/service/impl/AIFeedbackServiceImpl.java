package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.ai.AIFeedbackRespDTO;
import com.mycropdiary.api.dto.ai.ReportAIMessageReqDTO;
import com.mycropdiary.api.dto.ai.UpdateAIFeedbackReqDTO;
import com.mycropdiary.api.entity.AIFeedback;
import com.mycropdiary.api.entity.AIFeedbackReply;
import com.mycropdiary.api.entity.AIMessage;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.mapper.AIFeedbackMapper;
import com.mycropdiary.api.repository.AIFeedbackRepository;
import com.mycropdiary.api.repository.AIMessageRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.service.AIFeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("userAIFeedbackService")
@Transactional
public class AIFeedbackServiceImpl implements AIFeedbackService {

    private final AIFeedbackRepository feedbackRepository;
    private final AIMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AIFeedbackMapper aiFeedbackMapper;

    @Autowired
    public AIFeedbackServiceImpl(
            AIFeedbackRepository feedbackRepository,
            AIMessageRepository messageRepository,
            UserRepository userRepository,
            AIFeedbackMapper aiFeedbackMapper) {
        this.feedbackRepository = feedbackRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.aiFeedbackMapper = aiFeedbackMapper != null ? aiFeedbackMapper : new AIFeedbackMapper();
    }

    public AIFeedbackServiceImpl(
            AIFeedbackRepository feedbackRepository,
            AIMessageRepository messageRepository,
            UserRepository userRepository) {
        this(feedbackRepository, messageRepository, userRepository, new AIFeedbackMapper());
    }

    @Override
    public AIFeedbackRespDTO reportAIMessage(Integer userId, Integer messageId, ReportAIMessageReqDTO req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng ID: " + userId));
        AIMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tin nhắn AI ID: " + messageId));

        if (!"assistant".equalsIgnoreCase(message.getSenderType())) {
            throw new BadRequestException("Chỉ có thể phản hồi hoặc báo cáo các câu trả lời từ AI.");
        }

        AIFeedback feedback = feedbackRepository.findByMessageIdAndUserId(messageId, userId)
                .orElseGet(() -> new AIFeedback(message, user, req.rating(), req.reason().trim()));
        feedback.setRating(req.rating());
        feedback.setFeedbackText(req.reason().trim());

        AIFeedback saved = feedbackRepository.save(feedback);

        if (saved.getReplies().isEmpty()) {
            AIFeedbackReply acknowledgment = new AIFeedbackReply(
                    saved,
                    null,
                    "Ban cố vấn chuyên gia VietGAP của MyCropDiary đã tiếp nhận phản hồi của bạn để kiểm định lại dữ liệu tri thức. Cảm ơn đóng góp của bạn!"
            );
            saved.getReplies().add(acknowledgment);
            saved = feedbackRepository.save(saved);
        }

        return aiFeedbackMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AIFeedbackRespDTO> getMyFeedbackHistory(Integer userId) {
        List<AIFeedback> list = feedbackRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return aiFeedbackMapper.toResponseList(list);
    }

    @Override
    public AIFeedbackRespDTO updateFeedback(Integer userId, Integer feedbackId, UpdateAIFeedbackReqDTO req) {
        AIFeedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phản hồi ID: " + feedbackId));
        if (!feedback.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền chỉnh sửa phản hồi này.");
        }
        feedback.setRating(req.rating());
        feedback.setFeedbackText(req.reason().trim());
        AIFeedback saved = feedbackRepository.save(feedback);
        return aiFeedbackMapper.toResponse(saved);
    }
}
