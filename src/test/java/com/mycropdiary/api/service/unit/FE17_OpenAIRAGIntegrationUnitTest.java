package com.mycropdiary.api.service.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycropdiary.api.dto.ai.AIMessageResp;
import com.mycropdiary.api.dto.ai.AskAIQuestionReq;
import com.mycropdiary.api.entity.AIConversation;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.cultivation.ChecklistRule;
import com.mycropdiary.api.entity.knowledge.KnowledgeArticle;
import com.mycropdiary.api.repository.*;
import com.mycropdiary.api.repository.cultivation.ChecklistRuleRepository;
import com.mycropdiary.api.repository.knowledge.KnowledgeArticleRepository;
import com.mycropdiary.api.service.ai.OpenAIService;
import com.mycropdiary.api.service.impl.AIAssistantServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenAI Integration & Dynamic RAG VietGAP Unit Tests")
class FE17_OpenAIRAGIntegrationUnitTest {

    @Mock
    private AIConversationRepository conversationRepository;

    @Mock
    private AIMessageRepository messageRepository;

    @Mock
    private AIFeedbackRepository feedbackRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private ChecklistRuleRepository checklistRuleRepository;

    @Mock
    private KnowledgeArticleRepository knowledgeArticleRepository;

    @Mock
    private OpenAIService openAIService;

    private AIAssistantServiceImpl aiAssistantService;
    private User testUser;
    private Farm testFarm;
    private AIConversation testConversation;

    @BeforeEach
    void setUp() {
        aiAssistantService = new AIAssistantServiceImpl(
                conversationRepository,
                messageRepository,
                feedbackRepository,
                userRepository,
                farmRepository,
                checklistRuleRepository,
                knowledgeArticleRepository,
                openAIService
        );

        testUser = new User();
        ReflectionTestUtils.setField(testUser, "id", 1);
        testUser.setFullName("Nguyễn Văn Nông Dân");

        testFarm = new Farm();
        ReflectionTestUtils.setField(testFarm, "id", 10);
        testFarm.setFarmName("Trang trại VietGAP Mẫu");

        testConversation = new AIConversation(testUser, testFarm, "Tư vấn bón phân VietGAP");
        ReflectionTestUtils.setField(testConversation, "id", 100);
    }

    @Test
    @DisplayName("OpenAIService: Khi chưa có API Key thì isConfigured() trả về false và không gọi ra ngoài")
    void openAIService_whenNotConfigured_shouldReturnEmpty() {
        OpenAIService service = new OpenAIService("", "gpt-4o-mini", "https://api.openai.com/v1", 25, new ObjectMapper());
        assertThat(service.isConfigured()).isFalse();

        Optional<String> answer = service.generateAnswer("system prompt", List.of(), "Làm sao bón phân?");
        assertThat(answer).isEmpty();
    }

    @Test
    @DisplayName("RAG: Khi OpenAI phản hồi thành công, AI lấy đúng tiêu chuẩn từ DB và gán nguồn trích dẫn")
    void askAI_whenOpenAISucceeds_shouldUseOpenAIWithRAGSources() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(conversationRepository.findByIdAndUserId(100, 1)).thenReturn(Optional.of(testConversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(100)).thenReturn(List.of());

        // Giả lập trong DB có ChecklistRule về phân bón
        ChecklistRule rule = new ChecklistRule(testFarm, testUser, "Nguyên tắc 4 đúng khi bón phân", "Kiểm soát dư lượng Nitrate trước thu hoạch", "Dinh dưỡng", true);
        when(checklistRuleRepository.findActiveByFarmId(10)).thenReturn(List.of(rule));

        // Giả lập trong DB có KnowledgeArticle
        KnowledgeArticle article = new KnowledgeArticle(testUser, "Kỹ thuật ủ phân hữu cơ vi sinh Trichoderma", "u-phan-huu-co", "Nội dung bài viết...");
        article.setSummary("Hướng dẫn các bước ủ nóng diệt mầm bệnh");
        when(knowledgeArticleRepository.findByStatusString("published")).thenReturn(List.of(article));

        // Mock OpenAI phản hồi
        when(openAIService.generateAnswer(any(), any(), any())).thenReturn(
                Optional.of("Theo quy chuẩn VietGAP TCVN 11892-1:2017 và tiêu chí 'Nguyên tắc 4 đúng khi bón phân', bạn cần ngừng bón phân đạm trước thu hoạch 15 ngày.")
        );
        when(openAIService.getModel()).thenReturn("gpt-4o-mini");

        AIMessageResp resp = aiAssistantService.askAIAssistant(1, new AskAIQuestionReq("Làm sao để bón phân đúng chuẩn VietGAP?", 100, 10));

        assertThat(resp).isNotNull();
        assertThat(resp.senderType()).isEqualTo("assistant");
        assertThat(resp.modelName()).isEqualTo("gpt-4o-mini (OpenAI RAG)");
        assertThat(resp.messageText()).contains("TCVN 11892-1:2017");
        assertThat(resp.sources()).isNotEmpty();
        assertThat(resp.sources()).anyMatch(s -> s.contains("Tiêu chí VietGAP: Nguyên tắc 4 đúng khi bón phân"));
        assertThat(resp.sources()).anyMatch(s -> s.contains("Cẩm nang: Kỹ thuật ủ phân hữu cơ vi sinh"));
    }

    @Test
    @DisplayName("Fallback an toàn: Khi OpenAI không có kết nối, AI tự động chuyển sang cơ chế Fallback không bị sập")
    void askAI_whenOpenAIFails_shouldFallbackGracefully() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(conversationRepository.findByIdAndUserId(100, 1)).thenReturn(Optional.of(testConversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(100)).thenReturn(List.of());
        when(checklistRuleRepository.findActiveByFarmId(10)).thenReturn(List.of());
        when(knowledgeArticleRepository.findByStatusString("published")).thenReturn(List.of());

        // Giả lập OpenAI không phản hồi (Optional.empty)
        when(openAIService.generateAnswer(any(), any(), any())).thenReturn(Optional.empty());

        AIMessageResp resp = aiAssistantService.askAIAssistant(1, new AskAIQuestionReq("Nguồn nước tưới thế nào là đạt chuẩn VietGAP?", 100, 10));

        assertThat(resp).isNotNull();
        assertThat(resp.senderType()).isEqualTo("assistant");
        assertThat(resp.modelName()).isEqualTo("VietGAP-Expert-Fallback");
        assertThat(resp.messageText()).contains("QCVN 39:2011/BTNMT");
        assertThat(resp.sources()).anyMatch(s -> s.contains("QCVN 39:2011/BTNMT"));
    }
}
