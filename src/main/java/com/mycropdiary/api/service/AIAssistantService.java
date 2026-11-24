package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.ai.*;

import java.util.List;

public interface AIAssistantService {

    AIConversationRespDTO createConversation(Integer userId, CreateAIConversationReqDTO req);

    List<AIConversationRespDTO> getMyConversations(Integer userId, String keyword);

    AIConversationDetailRespDTO getConversationDetail(Integer userId, Integer conversationId);

    AIMessageRespDTO askAIAssistant(Integer userId, AskAIQuestionReqDTO req);

    void deleteConversation(Integer userId, Integer conversationId);

    FarmingDraftRespDTO parseVoiceActivity(GenerateDraftReqDTO req);
}
