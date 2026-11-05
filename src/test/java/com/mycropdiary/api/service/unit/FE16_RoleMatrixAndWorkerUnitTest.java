package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.ai.AIFeedbackResp;
import com.mycropdiary.api.dto.ai.ReportAIMessageReq;
import com.mycropdiary.api.dto.warehouse.CreateInventoryReq;
import com.mycropdiary.api.dto.warehouse.CreateWarehouseReq;
import com.mycropdiary.api.dto.warehouse.InventoryItemResp;
import com.mycropdiary.api.dto.warehouse.WarehouseResp;
import com.mycropdiary.api.dto.worker.FarmWorkerReq;
import com.mycropdiary.api.dto.worker.FarmWorkerResp;
import com.mycropdiary.api.entity.AIConversation;
import com.mycropdiary.api.entity.AIFeedback;
import com.mycropdiary.api.entity.AIMessage;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.cultivation.FarmWorker;
import com.mycropdiary.api.entity.cultivation.Inventory;
import com.mycropdiary.api.entity.cultivation.Material;
import com.mycropdiary.api.entity.cultivation.Warehouse;
import com.mycropdiary.api.repository.AIConversationRepository;
import com.mycropdiary.api.repository.AIFeedbackRepository;
import com.mycropdiary.api.repository.AIMessageRepository;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.repository.cultivation.FarmWorkerRepository;
import com.mycropdiary.api.repository.cultivation.InventoryRepository;
import com.mycropdiary.api.repository.cultivation.MaterialRepository;
import com.mycropdiary.api.repository.cultivation.WarehouseRepository;
import com.mycropdiary.api.service.impl.AIAssistantServiceImpl;
import com.mycropdiary.api.service.impl.FarmWorkerServiceImpl;
import com.mycropdiary.api.service.impl.WarehouseServiceImpl;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Role-based Operations Unit Tests (Worker Records, Warehouse/Inventory Full CRUD, AI Conversations/Feedback)")
public class FE16_RoleMatrixAndWorkerUnitTest {

    @Mock
    private FarmWorkerRepository farmWorkerRepository;
    @Mock
    private FarmMemberRepository farmMemberRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private FarmRepository farmRepository;
    @Mock
    private AIConversationRepository conversationRepository;
    @Mock
    private AIMessageRepository messageRepository;
    @Mock
    private AIFeedbackRepository feedbackRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FarmAccessGuard accessGuard;

    private FarmWorkerServiceImpl farmWorkerService;
    private WarehouseServiceImpl warehouseService;
    private AIAssistantServiceImpl aiAssistantService;

    private Farm testFarm;
    private User testUser;
    private FarmMember testMember;
    private FarmWorker testWorker;
    private Warehouse testWarehouse;
    private Material testMaterial;
    private Inventory testInventory;

    @BeforeEach
    void setUp() {
        farmWorkerService = new FarmWorkerServiceImpl(farmWorkerRepository, farmMemberRepository, accessGuard);
        warehouseService = new WarehouseServiceImpl(warehouseRepository, inventoryRepository, materialRepository, farmRepository, accessGuard);
        aiAssistantService = new AIAssistantServiceImpl(
                conversationRepository, messageRepository, feedbackRepository, userRepository, farmRepository
        );

        testFarm = new Farm();
        testFarm.setId(10);
        testFarm.setFarmName("Trang trại VietGAP 1");

        testUser = new User("worker@test.com", "hash", "Nguyen Van A", "0901234567", "user", "active");
        testUser.setId(100);

        testMember = new FarmMember(testFarm, testUser, "staff", "active");
        testMember.setId(20);

        testWorker = new FarmWorker(testMember, "CN-001", "Công nhân kỹ thuật", "active", LocalDate.now(), "Ghi chú");
        testWorker.setId(5);

        testWarehouse = new Warehouse(testFarm, "Kho A", "Địa chỉ A", null);
        testWarehouse.setId(30);

        testMaterial = new Material();
        testMaterial.setId(40);
        testMaterial.setMaterialName("Phân NPK");
        testMaterial.setUnit("kg");

        testInventory = new Inventory(testWarehouse, testMaterial, BigDecimal.valueOf(100), BigDecimal.valueOf(20));
        testInventory.setId(50);
    }

    // --- UC-15: Worker Records Tests ---
    @Test
    @DisplayName("UC15-1: Lấy danh sách hồ sơ công nhân thành công")
    void testGetWorkersByFarm() {
        when(farmWorkerRepository.findByFarmId(10)).thenReturn(List.of(testWorker));

        List<FarmWorkerResp> list = farmWorkerService.getWorkersByFarm(100, 10, null, null);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("CN-001", list.get(0).workerCode());
        verify(accessGuard).requireFarmAccess(100, 10);
    }

    @Test
    @DisplayName("UC15-2: Tạo hồ sơ công nhân mới thành công")
    void testCreateWorker_success() {
        FarmWorkerReq req = new FarmWorkerReq(20, "CN-002", "Công nhân chăm sóc", "active", LocalDate.now(), "Không");
        when(farmMemberRepository.findById(20)).thenReturn(Optional.of(testMember));
        when(farmWorkerRepository.findByFarmMemberId(20)).thenReturn(Optional.empty());
        when(farmWorkerRepository.existsByFarmMemberFarmIdAndWorkerCodeIgnoreCase(10, "CN-002")).thenReturn(false);
        when(farmWorkerRepository.save(any(FarmWorker.class))).thenAnswer(invocation -> {
            FarmWorker w = invocation.getArgument(0);
            w.setId(6);
            return w;
        });

        FarmWorkerResp resp = farmWorkerService.createWorker(100, 10, req);

        assertNotNull(resp);
        assertEquals(6, resp.workerId());
        assertEquals("CN-002", resp.workerCode());
        verify(farmWorkerRepository).save(any(FarmWorker.class));
    }

    @Test
    @DisplayName("UC15-3: Cập nhật và xóa hồ sơ công nhân thành công")
    void testUpdateAndDeleteWorker_success() {
        when(farmWorkerRepository.findByIdAndFarmMemberFarmId(5, 10)).thenReturn(Optional.of(testWorker));
        when(farmWorkerRepository.save(any(FarmWorker.class))).thenReturn(testWorker);

        FarmWorkerReq updateReq = new FarmWorkerReq(20, "CN-001", "Trưởng nhóm kỹ thuật", "active", LocalDate.now(), "Mới");
        FarmWorkerResp updated = farmWorkerService.updateWorker(100, 10, 5, updateReq);
        assertEquals("Trưởng nhóm kỹ thuật", updated.jobTitle());

        farmWorkerService.deleteWorker(100, 10, 5);
        verify(farmWorkerRepository).delete(testWorker);
    }

    // --- UC-32: Warehouse Update & Delete Tests ---
    @Test
    @DisplayName("UC32: Cập nhật và xóa kho hàng thành công")
    void testUpdateAndDeleteWarehouse_success() {
        when(warehouseRepository.findById(30)).thenReturn(Optional.of(testWarehouse));
        when(warehouseRepository.save(any(Warehouse.class))).thenReturn(testWarehouse);

        CreateWarehouseReq req = new CreateWarehouseReq("Kho A Mới", "Địa chỉ mới", null);
        WarehouseResp resp = warehouseService.updateWarehouse(100, 30, req);
        assertEquals("Kho A Mới", resp.warehouseName());

        when(inventoryRepository.findByWarehouseId(30)).thenReturn(List.of(testInventory));
        warehouseService.deleteWarehouse(100, 30);

        verify(inventoryRepository).deleteAll(List.of(testInventory));
        verify(warehouseRepository).delete(testWarehouse);
    }

    // --- UC-33: Inventory Create & Delete Tests ---
    @Test
    @DisplayName("UC33: Thêm và xóa bản ghi tồn kho vật tư thành công")
    void testCreateAndDeleteInventory_success() {
        when(warehouseRepository.findById(30)).thenReturn(Optional.of(testWarehouse));
        when(materialRepository.findByIdAndFarmId(40, 10)).thenReturn(Optional.of(testMaterial));
        when(inventoryRepository.findByWarehouseIdAndMaterialId(30, 40)).thenReturn(Optional.empty());
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> {
            Inventory inv = invocation.getArgument(0);
            inv.setId(51);
            return inv;
        });

        CreateInventoryReq req = new CreateInventoryReq(30, 40, BigDecimal.valueOf(50), BigDecimal.valueOf(10));
        InventoryItemResp item = warehouseService.createInventory(100, 10, req);

        assertNotNull(item);
        assertEquals(51, item.inventoryId());

        when(inventoryRepository.findById(50)).thenReturn(Optional.of(testInventory));
        warehouseService.deleteInventory(100, 50);
        verify(inventoryRepository).delete(testInventory);
    }

    // --- UC-12 & UC-13: AI Conversations Delete & Feedback Update Tests ---
    @Test
    @DisplayName("UC12 & UC13: Xóa cuộc hội thoại và cập nhật phản hồi AI thành công")
    void testAIConversationDeleteAndFeedbackUpdate_success() {
        AIConversation conv = new AIConversation(testUser, testFarm, "Tư vấn bón phân VietGAP");
        conv.setId(1);
        when(conversationRepository.findByIdAndUserId(1, 100)).thenReturn(Optional.of(conv));

        AIMessage msg = new AIMessage(conv, "assistant", "Bón NPK theo tỷ lệ 16-16-8", "VietGAP-LLM-v2");
        msg.setId(11);
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(1)).thenReturn(List.of(msg));

        AIFeedback feedback = new AIFeedback(msg, testUser, 3, "Chưa đủ thông tin");
        feedback.setId(21);
        when(feedbackRepository.findByMessageIdIn(List.of(11))).thenReturn(List.of(feedback));

        aiAssistantService.deleteConversation(100, 1);

        verify(feedbackRepository).deleteAll(List.of(feedback));
        verify(messageRepository).deleteAll(List.of(msg));
        verify(conversationRepository).delete(conv);

        // Test update feedback
        when(feedbackRepository.findById(21)).thenReturn(Optional.of(feedback));
        when(feedbackRepository.save(any(AIFeedback.class))).thenReturn(feedback);

        ReportAIMessageReq updateReq = new ReportAIMessageReq(5, "Đã hiểu rõ hơn sau khi hỏi lại");
        AIFeedbackResp updatedFeedback = aiAssistantService.updateFeedback(100, 21, updateReq);

        assertNotNull(updatedFeedback);
        assertEquals(5, updatedFeedback.rating());
        assertEquals("Đã hiểu rõ hơn sau khi hỏi lại", updatedFeedback.feedbackText());
    }
}
