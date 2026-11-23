package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.controller.AdminChecklistRuleController;
import com.mycropdiary.api.controller.AdminChecklistRuleController.AdminChecklistRuleReq;
import com.mycropdiary.api.controller.AdminChecklistRuleController.AdminChecklistRuleResp;
import com.mycropdiary.api.controller.AdminCropCategoryController;
import com.mycropdiary.api.controller.AdminCropCategoryController.CropCategoryReq;
import com.mycropdiary.api.dto.admin.AdminAIFeedbackResp;
import com.mycropdiary.api.dto.admin.AdminStatisticsResp;
import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.cultivation.CropCategoryResp;
import com.mycropdiary.api.entity.AIFeedback;
import com.mycropdiary.api.entity.AIMessage;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.cultivation.ChecklistRule;
import com.mycropdiary.api.entity.cultivation.CropCategory;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.AIFeedbackRepository;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.repository.cultivation.ChecklistRuleRepository;
import com.mycropdiary.api.repository.cultivation.CropCategoryRepository;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Admin Management Unit Tests (Statistics, AI Feedback, Crop Categories, Checklist Rules - Commit 277356d)")
public class FE13_AdminSystemManagementUnitTest {

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

    @Mock
    private CropCategoryRepository cropCategoryRepository;

    @Mock
    private ChecklistRuleRepository checklistRuleRepository;

    private AdminStatisticsServiceImpl adminStatisticsService;
    private AdminAIFeedbackServiceImpl adminAIFeedbackService;
    private AdminCropCategoryController adminCropCategoryController;
    private AdminChecklistRuleController adminChecklistRuleController;

    @BeforeEach
    void setUp() {
        adminStatisticsService = new AdminStatisticsServiceImpl(
                userRepository, farmMemberRepository, farmRegistrationRepository,
                farmRepository, cropSeasonRepository, aiFeedbackRepository
        );
        adminAIFeedbackService = new AdminAIFeedbackServiceImpl(
                aiFeedbackRepository, userRepository
        );
        adminCropCategoryController = new AdminCropCategoryController(cropCategoryRepository);
        adminChecklistRuleController = new AdminChecklistRuleController(checklistRuleRepository);
    }

    // --- 1. Admin Statistics Tests ---
    @Test
    @DisplayName("FE13_SYS_UTC01 [Normal] - View System Statistics: Trả về đầy đủ số liệu tổng hợp hệ thống")
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
    @DisplayName("FE13_SYS_UTC02 [Boundary] - View System Statistics: Cơ sở dữ liệu mới khởi tạo (0 dữ liệu) -> Trả về 0 an toàn")
    void testGetSystemStatistics_emptyDatabase_shouldReturnZeroesGracefully() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.countByAccountStatusIgnoreCase("ACTIVE")).thenReturn(0L);
        when(userRepository.countByAccountStatusIgnoreCase("LOCKED")).thenReturn(0L);
        when(userRepository.countBySystemRoleIgnoreCase("OWNER")).thenReturn(0L);
        when(userRepository.countBySystemRoleIgnoreCase("STAFF")).thenReturn(0L);
        when(farmRegistrationRepository.count()).thenReturn(0L);
        when(farmRegistrationRepository.countByRegistrationStatusIgnoreCase("PENDING")).thenReturn(0L);
        when(farmRegistrationRepository.countByRegistrationStatusIgnoreCase("APPROVED")).thenReturn(0L);
        when(farmRegistrationRepository.countByRegistrationStatusIgnoreCase("REJECTED")).thenReturn(0L);
        when(farmRepository.count()).thenReturn(0L);
        when(farmRepository.findAll()).thenReturn(List.of());
        when(cropSeasonRepository.count()).thenReturn(0L);
        when(cropSeasonRepository.countBySeasonStatusIgnoreCase("ACTIVE")).thenReturn(0L);
        when(cropSeasonRepository.countBySeasonStatusIgnoreCase("IN_PROGRESS")).thenReturn(0L);
        when(cropSeasonRepository.countBySeasonStatusIgnoreCase("COMPLETED")).thenReturn(0L);
        when(aiFeedbackRepository.count()).thenReturn(0L);
        when(aiFeedbackRepository.countByRepliesIsEmpty()).thenReturn(0L);
        when(aiFeedbackRepository.countByRepliesIsNotEmpty()).thenReturn(0L);
        when(aiFeedbackRepository.getAverageRating()).thenReturn(0.0);

        AdminStatisticsResp stats = adminStatisticsService.getSystemStatistics();
        assertNotNull(stats);
        assertEquals(0L, stats.accounts().totalUsers());
        assertEquals(0L, stats.farms().totalFarms());
        assertEquals(0.0, stats.aiFeedback().averageRating());
    }

    // --- 2. Admin AI Feedback Tests ---
    @Test
    @DisplayName("FE13_SYS_UTC03 [Normal] - Manage AI Feedback: Xem danh sách phản hồi người dùng")
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
    @DisplayName("FE13_SYS_UTC04 [Normal] - Manage AI Feedback: Admin phản hồi ý kiến thành công")
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
    @DisplayName("FE13_SYS_UTC05 [Abnormal] - Manage AI Feedback: Phản hồi ID feedback không tồn tại -> Báo lỗi ResourceNotFound")
    void testReplyFeedback_notFound_throwsResourceNotFound() {
        when(aiFeedbackRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> adminAIFeedbackService.replyFeedback(1, 999, "Tra loi"));
    }

    @Test
    @DisplayName("FE13_SYS_UTC06 [Normal] - Manage AI Feedback: Cập nhật trạng thái phản hồi (RESOLVED)")
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

    @Test
    @DisplayName("FE13_SYS_UTC07 [Abnormal] - Manage AI Feedback: Cập nhật trạng thái ID không tồn tại -> Ném ResourceNotFoundException")
    void testUpdateStatus_notFound_throwsResourceNotFound() {
        when(aiFeedbackRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> adminAIFeedbackService.updateFeedbackStatus(1, 999, "DISMISSED"));
    }

    // --- 3. Admin Crop Category Tests (Commit 277356d) ---
    @Test
    @DisplayName("FE13_SYS_UTC08 [Normal] - Admin Crop Category: Lấy danh mục cây trồng dùng chung")
    void testGetCropCategories_success() {
        CropCategory cat = new CropCategory("Dưa lưới", "Cây ăn quả ngắn ngày VietGAP");
        cat.setId(1);
        cat.setImageUrl("https://example.com/melon.jpg");
        when(cropCategoryRepository.findAll()).thenReturn(List.of(cat));

        ApiResp<List<CropCategoryResp>> resp = adminCropCategoryController.getCropCategories();
        assertNotNull(resp);
        assertTrue(resp.success());
        assertEquals(1, resp.data().size());
        assertEquals("Dưa lưới", resp.data().get(0).categoryName());
    }

    @Test
    @DisplayName("FE13_SYS_UTC09 [Normal] - Admin Crop Category: Tạo mới danh mục cây trồng thành công")
    void testCreateCropCategory_success() {
        CropCategoryReq req = new CropCategoryReq("Cà chua Cherry", "Trồng trong nhà màng", "https://example.com/tomato.jpg");
        CropCategory saved = new CropCategory(req.categoryName(), req.description());
        saved.setId(2);
        saved.setImageUrl(req.imageUrl());

        when(cropCategoryRepository.save(any(CropCategory.class))).thenReturn(saved);

        ApiResp<CropCategoryResp> resp = adminCropCategoryController.createCropCategory(req);
        assertNotNull(resp);
        assertEquals(2, resp.data().id());
        assertEquals("Cà chua Cherry", resp.data().categoryName());
    }

    @Test
    @DisplayName("FE13_SYS_UTC10 [Normal] - Admin Crop Category: Cập nhật thông tin danh mục cây trồng")
    void testUpdateCropCategory_success() {
        CropCategory existing = new CropCategory("Dưa lưới", "Mô tả cũ");
        existing.setId(1);

        when(cropCategoryRepository.findById(1)).thenReturn(Optional.of(existing));
        when(cropCategoryRepository.save(any(CropCategory.class))).thenReturn(existing);

        CropCategoryReq updateReq = new CropCategoryReq("Dưa lưới Hoàng Kim", "Mô tả mới", "https://example.com/new.jpg");
        ApiResp<CropCategoryResp> resp = adminCropCategoryController.updateCropCategory(1, updateReq);

        assertNotNull(resp);
        assertEquals("Dưa lưới Hoàng Kim", resp.data().categoryName());
    }

    @Test
    @DisplayName("FE13_SYS_UTC11 [Abnormal] - Admin Crop Category: Cập nhật ID không tồn tại -> Ném ResourceNotFoundException")
    void testUpdateCropCategory_notFound_throwsResourceNotFound() {
        when(cropCategoryRepository.findById(999)).thenReturn(Optional.empty());
        CropCategoryReq req = new CropCategoryReq("Test", "Test", null);
        assertThrows(ResourceNotFoundException.class, () -> adminCropCategoryController.updateCropCategory(999, req));
    }

    // --- 4. Admin Checklist Rule Tests (Commit 277356d) ---
    @Test
    @DisplayName("FE13_SYS_UTC12 [Normal] - Admin Checklist Rule: Xem danh sách quy tắc kiểm tra đánh giá VietGAP")
    void testGetChecklistRules_success() {
        ChecklistRule rule = new ChecklistRule();
        rule.setId(10);
        rule.setRuleName("Kiểm soát nguồn nước tưới");
        rule.setDescription("Nước tưới phải đạt chuẩn QCVN 39:2011/BTNMT");
        rule.setCategory("Nước");
        rule.setIsActive(true);

        when(checklistRuleRepository.findAll()).thenReturn(List.of(rule));

        ApiResp<List<AdminChecklistRuleResp>> resp = adminChecklistRuleController.getChecklistRules();
        assertNotNull(resp);
        assertEquals(1, resp.data().size());
        assertEquals("Kiểm soát nguồn nước tưới", resp.data().get(0).ruleName());
    }

    @Test
    @DisplayName("FE13_SYS_UTC13 [Normal] - Admin Checklist Rule: Thêm mới quy tắc bảng kiểm thành công")
    void testCreateChecklistRule_success() {
        AdminChecklistRuleReq req = new AdminChecklistRuleReq(
                "Kiểm soát dư lượng thuốc BVTV", "Không phun trước thu hoạch 14 ngày", "Hóa chất", true
        );
        ChecklistRule saved = new ChecklistRule();
        saved.setId(11);
        saved.setRuleName(req.ruleName());
        saved.setDescription(req.description());
        saved.setCategory(req.category());
        saved.setIsActive(true);

        when(checklistRuleRepository.save(any(ChecklistRule.class))).thenReturn(saved);

        ApiResp<AdminChecklistRuleResp> resp = adminChecklistRuleController.createChecklistRule(req);
        assertNotNull(resp);
        assertEquals(11, resp.data().ruleId());
        assertEquals("Kiểm soát dư lượng thuốc BVTV", resp.data().ruleName());
    }

    @Test
    @DisplayName("FE13_SYS_UTC14 [Normal] - Admin Checklist Rule: Cập nhật quy tắc bảng kiểm thành công")
    void testUpdateChecklistRule_success() {
        ChecklistRule existing = new ChecklistRule();
        existing.setId(10);
        existing.setRuleName("Quy tắc cũ");
        existing.setDescription("Mô tả cũ");
        existing.setIsActive(true);

        when(checklistRuleRepository.findById(10)).thenReturn(Optional.of(existing));
        when(checklistRuleRepository.save(any(ChecklistRule.class))).thenReturn(existing);

        AdminChecklistRuleReq req = new AdminChecklistRuleReq("Quy tắc mới", "Mô tả mới", "Đất", false);
        ApiResp<AdminChecklistRuleResp> resp = adminChecklistRuleController.updateChecklistRule(10, req);

        assertNotNull(resp);
        assertEquals("Quy tắc mới", resp.data().ruleName());
        assertFalse(resp.data().isActive());
    }

    @Test
    @DisplayName("FE13_SYS_UTC15 [Abnormal] - Admin Checklist Rule: Cập nhật quy tắc ID không tồn tại -> Ném ResourceNotFoundException")
    void testUpdateChecklistRule_notFound_throwsResourceNotFound() {
        when(checklistRuleRepository.findById(999)).thenReturn(Optional.empty());
        AdminChecklistRuleReq req = new AdminChecklistRuleReq("Rule", "Desc", "Cat", true);
        assertThrows(ResourceNotFoundException.class, () -> adminChecklistRuleController.updateChecklistRule(999, req));
    }
}
