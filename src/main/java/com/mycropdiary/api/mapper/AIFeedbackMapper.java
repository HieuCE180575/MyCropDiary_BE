package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.admin.AIFeedbackDetailRespDTO;
import com.mycropdiary.api.dto.ai.AIFeedbackRespDTO;
import com.mycropdiary.api.entity.AIFeedback;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AIFeedbackMapper {

    public AIFeedbackRespDTO toResponse(AIFeedback entity) {
        return AIFeedbackRespDTO.fromEntity(entity);
    }

    public List<AIFeedbackRespDTO> toResponseList(List<AIFeedback> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).toList();
    }

    public AIFeedbackDetailRespDTO toDetailResponse(AIFeedback f) {
        if (f == null) {
            return null;
        }
        String msgText = f.getMessage() != null ? f.getMessage().getMessageText() : null;
        String userQuestion = (f.getMessage() != null && f.getMessage().getConversation() != null)
                ? f.getMessage().getConversation().getConversationTitle()
                : null;
        String senderName = f.getUser() != null ? f.getUser().getFullName() : "Nông dân";
        String senderEmail = f.getUser() != null ? f.getUser().getEmail() : null;
        Integer userId = f.getUser() != null ? f.getUser().getId() : null;
        String status = (f.getReplies() == null || f.getReplies().isEmpty()) ? "OPEN" : "RESOLVED";

        List<AIFeedbackDetailRespDTO.ReplyItem> replies = f.getReplies() == null ? List.of() :
                f.getReplies().stream().map(r -> new AIFeedbackDetailRespDTO.ReplyItem(
                        r.getId(),
                        r.getReplyText(),
                        r.getUser() != null ? r.getUser().getFullName() : "Quản trị viên",
                        r.getCreatedAt()
                )).toList();

        return new AIFeedbackDetailRespDTO(
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
