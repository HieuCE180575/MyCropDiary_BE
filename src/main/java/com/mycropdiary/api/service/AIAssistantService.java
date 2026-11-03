package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.ai.*;

import java.util.List;

public interface AIAssistantService {

    AIConversationResp createConversation(Integer userId, CreateAIConversationReq req);

    List<AIConversationResp> getMyConversations(Integer userId, String keyword);

    AIConversationDetailResp getConversationDetail(Integer userId, Integer conversationId);

    AIMessageResp askAIAssistant(Integer userId, AskAIQuestionReq req);

    AIFeedbackResp reportAIMessage(Integer userId, Integer messageId, ReportAIMessageReq req);

    List<AIFeedbackResp> getMyFeedbackHistory(Integer userId);
}

