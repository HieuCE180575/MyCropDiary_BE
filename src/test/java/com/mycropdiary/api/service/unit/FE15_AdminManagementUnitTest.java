package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.admin.AdminAIFeedbackResp;
import com.mycropdiary.api.dto.admin.AdminStatisticsResp;
import com.mycropdiary.api.entity.AIFeedback;
import com.mycropdiary.api.entity.AIMessage;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.repository.AIFeedbackRepository;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.repository.cultivation.CropSeasonRepository;
import com.mycropdiary.api.repository.farmregistration.FarmRegistrationRepository;
import com.mycropdiary.api.service.admin.impl.AdminAIFeedbackServiceImpl;
import com.mycropdiary.api.service.admin.impl.AdminStatisticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Admin Management Unit Tests (Statistics & AI Feedback)")
public class FE15_AdminManagementUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FarmMemberRepository farmMemberRepository;

    @Mock
    private FarmRegistrationRepository farmRegistrationRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private CropSeasonRepository cropSeasonRepository;

    @Mock
    private AIFeedbackRepository aiFeedbackRepository;

    private AdminStatisticsServiceImpl adminStatisticsService;
    private AdminAIFeedbackServiceImpl adminAIFeedbackService;

    @BeforeEach
    void setUp() {
        adminStatisticsService = new AdminStatisticsServiceImpl(
                userRepository, farmMemberRepository, farmRegistrationRepository,
                farmRepository, cropSeasonRepository, aiFeedbackRepository
        );
        adminAIFeedbackService = new AdminAIFeedbackServiceImpl(
                aiFeedbackRepository, userRepository
        );
    }

    @Test
    @DisplayName("View System Statistics - Overview")
    void testGetSystemStatistics_success() {
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByAccountStatusIgnoreCase("ACTIVE")).thenReturn(90L);
        when(userRepository.countByAccountStatusIgnoreCase("LOCKED")).thenReturn(10L);
        when(userRepository.countBySystemRoleIgnoreCase("OWNER")).thenReturn(30L);
        when(userRepository.countBySystemRoleIgnoreCase("STAFF")).thenReturn(50L);

        when(farmRegistrationRepository.count()).thenReturn(40L);
        when(farmRegistrationRepository.countByRegistrationStatusIgnoreCase("PENDING")).thenReturn(5L);
        when(farmRegistrationRepository.countByRegistrationStatusIgnoreCase("APPROVED")).thenReturn(30L);
        when(farmRegistrationRepository.countByRegistrationStatusIgnoreCase("REJECTED")).thenReturn(5L);

        when(farmRepository.count()).thenReturn(25L);
        when(farmRepository.findAll()).thenReturn(List.of());

        when(cropSeasonRepository.count()).thenReturn(60L);
        when(cropSeasonRepository.countBySeasonStatusIgnoreCase("ACTIVE")).thenReturn(20L);
        when(cropSeasonRepository.countBySeasonStatusIgnoreCase("IN_PROGRESS")).thenReturn(5L);
        when(cropSeasonRepository.countBySeasonStatusIgnoreCase("COMPLETED")).thenReturn(35L);

        when(aiFeedbackRepository.count()).thenReturn(15L);
        when(aiFeedbackRepository.countByRepliesIsEmpty()).thenReturn(3L);
        when(aiFeedbackRepository.countByRepliesIsNotEmpty()).thenReturn(12L);
        when(aiFeedbackRepository.getAverageRating()).thenReturn(4.5);

        AdminStatisticsResp stats = adminStatisticsService.getSystemStatistics();
        assertNotNull(stats);
        assertEquals(100L, stats.accounts().totalUsers());
        assertEquals(90L, stats.accounts().activeUsers());
        assertEquals(5L, stats.registrations().pendingRegistrations());
        assertEquals(25L, stats.farms().totalFarms());
        assertEquals(60L, stats.seasons().totalSeasons());
        assertEquals(15L, stats.aiFeedback().totalFeedbacks());
        assertEquals(4.5, stats.aiFeedback().averageRating());
    }

    @Test
    @DisplayName("Manage AI Feedback - View Feedback List")
    void testGetAllFeedbacks_success() {
        User user = new User("farmer@test.com", "hash", "Farmer A", "0901234567", "USER", "ACTIVE");
        user.setId(10);
        AIMessage msg = new AIMessage();
        msg.setId(100);
        msg.setMessageText("Tu van cay trong");

        AIFeedback feedback = new AIFeedback(msg, user, 4, "Can them tai lieu");
        feedback.setId(1);
        feedback.setReplies(new ArrayList<>());

        when(aiFeedbackRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(feedback));

        List<AdminAIFeedbackResp> list = adminAIFeedbackService.getAllFeedbacks(null, null);
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Farmer A", list.get(0).senderName());
        assertEquals("OPEN", list.get(0).status());
    }

    @Test
    @DisplayName("Manage AI Feedback - Reply Feedback")
    void testReplyFeedback_success() {
        User admin = new User("admin@test.com", "hash", "Admin", "0909999999", "ADMIN", "ACTIVE");
        admin.setId(1);
        User user = new User("farmer@test.com", "hash", "Farmer A", "0901234567", "USER", "ACTIVE");
        user.setId(10);
        AIMessage msg = new AIMessage();
        msg.setId(100);

        AIFeedback feedback = new AIFeedback(msg, user, 3, "Chua ro");
        feedback.setId(2);
        feedback.setReplies(new ArrayList<>());

        when(aiFeedbackRepository.findById(2)).thenReturn(Optional.of(feedback));
        when(userRepository.findById(1)).thenReturn(Optional.of(admin));
        when(aiFeedbackRepository.save(any(AIFeedback.class))).thenReturn(feedback);

        AdminAIFeedbackResp resp = adminAIFeedbackService.replyFeedback(1, 2, "Da cap nhat huong dan.");
        assertNotNull(resp);
        assertEquals(1, resp.replies().size());
        assertEquals("Da cap nhat huong dan.", resp.replies().get(0).replyText());
    }

    @Test
    @DisplayName("Manage AI Feedback - Update Status")
    void testUpdateStatus_success() {
        User user = new User("farmer@test.com", "hash", "Farmer A", "0901234567", "USER", "ACTIVE");
        user.setId(10);
        AIMessage msg = new AIMessage();
        msg.setId(100);

        AIFeedback feedback = new AIFeedback(msg, user, 3, "Chua ro");
        feedback.setId(3);
        feedback.setReplies(new ArrayList<>());

        when(aiFeedbackRepository.findById(3)).thenReturn(Optional.of(feedback));
        when(aiFeedbackRepository.save(any(AIFeedback.class))).thenReturn(feedback);

        AdminAIFeedbackResp resp = adminAIFeedbackService.updateFeedbackStatus(1, 3, "RESOLVED");
        assertNotNull(resp);
        assertEquals("RESOLVED", resp.status());
    }
}
