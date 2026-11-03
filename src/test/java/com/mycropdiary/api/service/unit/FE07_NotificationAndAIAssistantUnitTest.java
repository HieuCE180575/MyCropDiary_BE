package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.ai.*;
import com.mycropdiary.api.dto.notification.NotificationListResp;
import com.mycropdiary.api.dto.notification.NotificationResp;
import com.mycropdiary.api.entity.*;
import com.mycropdiary.api.repository.*;
import com.mycropdiary.api.service.impl.AIAssistantServiceImpl;
import com.mycropdiary.api.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE07 - Notification and AI Assistant Service Unit Tests (UC-10, UC-12, UC-13)")
class FE07_NotificationAndAIAssistantUnitTest {

    @Mock
    private NotificationRepository notificationRepository;

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

    private NotificationServiceImpl notificationService;
    private AIAssistantServiceImpl aiAssistantService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(notificationRepository, userRepository);
        aiAssistantService = new AIAssistantServiceImpl(
                conversationRepository,
                messageRepository,
                feedbackRepository,
                userRepository,
                farmRepository
        );

        mockUser = new User("farmer@example.com", "hash", "Tran Van Farmer", "0901234567", "user", "active");
        mockUser.setId(10);
    }

    // ==========================================
    // UC-10: Notifications
    // ==========================================

    @Test
    @DisplayName("UC-10: View Notification - Return existing notifications and unread count")
    void testGetMyNotifications_Success() {
        Notification n = new Notification(mockUser, "Lịch bón phân", "Đã đến lịch bón phân", "TASK", "/tasks");
        n.setId(1);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(10), any(Pageable.class)))
                .thenReturn(List.of(n));
        when(notificationRepository.countByUserIdAndIsReadFalse(10)).thenReturn(1L);

        NotificationListResp resp = notificationService.getMyNotifications(10, 20);

        assertNotNull(resp);
        assertEquals(1, resp.items().size());
        assertEquals("Lịch bón phân", resp.items().get(0).title());
        assertEquals(1L, resp.unreadCount());
    }

    @Test
    @DisplayName("UC-10: Mark Notification as Read")
    void testMarkAsRead_Success() {
        Notification n = new Notification(mockUser, "Cảnh báo sâu bệnh", "Xuất hiện bọ trĩ", "VIETGAP", "/zones");
        n.setId(2);
        n.setIsRead(false);

        when(notificationRepository.findByIdAndUserId(2, 10)).thenReturn(Optional.of(n));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResp resp = notificationService.markAsRead(10, 2);

        assertNotNull(resp);
        assertTrue(resp.isRead());
        verify(notificationRepository, times(1)).save(n);
    }

    // ==========================================
    // UC-12: AI Assistant & Conversations
    // ==========================================

    @Test
    @DisplayName("UC-12.1: Ask AI Assistant - Answers pest query with IPM advice")
    void testAskAIAssistant_PestQuery() {
        when(userRepository.findById(10)).thenReturn(Optional.of(mockUser));

        AIConversation conversation = new AIConversation(mockUser, null, "Hỏi về bọ trĩ");
        conversation.setId(101);

        when(conversationRepository.findByIdAndUserId(101, 10)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(AIMessage.class))).thenAnswer(inv -> {
            AIMessage m = inv.getArgument(0);
            m.setId(201);
            return m;
        });
        when(conversationRepository.save(any(AIConversation.class))).thenReturn(conversation);

        AskAIQuestionReq req = new AskAIQuestionReq("Bọ trĩ trên dưa lưới trị bằng gì theo VietGAP?", 101, null);
        AIMessageResp resp = aiAssistantService.askAIAssistant(10, req);

        assertNotNull(resp);
        assertEquals("assistant", resp.senderType());
        assertTrue(resp.messageText().contains("Beauveria bassiana") || resp.messageText().contains("Neem"));
        assertTrue(resp.sources().stream().anyMatch(s -> s.contains("TCVN 11892-1:2017")));
    }

    @Test
    @DisplayName("UC-12.2 & UC-12.3: View & Search My Conversations")
    void testGetMyConversations_WithSearch() {
        AIConversation c1 = new AIConversation(mockUser, null, "Tư vấn bón phân đạm");
        c1.setId(102);

        when(conversationRepository.searchByUserIdAndKeyword(10, "phân")).thenReturn(List.of(c1));
        when(messageRepository.countByConversationId(102)).thenReturn(4L);
        when(messageRepository.findTopByConversationIdOrderByCreatedAtDesc(102))
                .thenReturn(Optional.of(new AIMessage(c1, "assistant", "Nên ngưng bón phân trước 15 ngày", "VietGAP-LLM-v2")));

        List<AIConversationResp> list = aiAssistantService.getMyConversations(10, "phân");

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Tư vấn bón phân đạm", list.get(0).title());
        assertEquals(4L, list.get(0).messageCount());
    }

    // ==========================================
    // UC-13: AI Feedback & Report
    // ==========================================

    @Test
    @DisplayName("UC-13.4: Report AI Response & UC-13.3: View Feedback History")
    void testReportAIMessage_AndFeedbackHistory() {
        when(userRepository.findById(10)).thenReturn(Optional.of(mockUser));

        AIConversation conversation = new AIConversation(mockUser, null, "Hỏi về nước tưới");
        conversation.setId(103);
        AIMessage aiMsg = new AIMessage(conversation, "assistant", "Thông tin kiểm nghiệm nước QCVN 39", "VietGAP-LLM-v2");
        aiMsg.setId(301);

        when(messageRepository.findById(301)).thenReturn(Optional.of(aiMsg));
        when(feedbackRepository.findByMessageIdAndUserId(301, 10)).thenReturn(Optional.empty());
        when(feedbackRepository.save(any(AIFeedback.class))).thenAnswer(inv -> {
            AIFeedback f = inv.getArgument(0);
            f.setId(401);
            return f;
        });

        ReportAIMessageReq req = new ReportAIMessageReq(2, "Thông số pH trong câu trả lời chưa chuẩn với rau ăn lá");
        AIFeedbackResp resp = aiAssistantService.reportAIMessage(10, 301, req);

        assertNotNull(resp);
        assertEquals(401, resp.feedbackId());
        assertEquals(2, resp.rating());
        assertFalse(resp.replies().isEmpty());

        // Test history
        AIFeedback savedFeedback = new AIFeedback(aiMsg, mockUser, 2, "Thông số pH chưa chuẩn");
        savedFeedback.setId(401);
        when(feedbackRepository.findByUserIdOrderByCreatedAtDesc(10)).thenReturn(List.of(savedFeedback));

        List<AIFeedbackResp> history = aiAssistantService.getMyFeedbackHistory(10);
        assertEquals(1, history.size());
        assertEquals(401, history.get(0).feedbackId());
    }
}

