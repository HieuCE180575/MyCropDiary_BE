package com.mycropdiary.api;

import com.mycropdiary.api.dto.assignment.AssignedAreaResponse;
import com.mycropdiary.api.dto.assignment.AssignmentResponse;
import com.mycropdiary.api.dto.assignment.CreateAssignmentRequest;
import com.mycropdiary.api.dto.plot.CreatePlotRequest;
import com.mycropdiary.api.dto.plot.PlotResponse;
import com.mycropdiary.api.dto.staff.AddStaffRequest;
import com.mycropdiary.api.dto.staff.FarmMemberResponse;
import com.mycropdiary.api.entity.AppUser;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.ProductionArea;
import com.mycropdiary.api.exception.BusinessRuleException;
import com.mycropdiary.api.exception.ResourceConflictException;
import com.mycropdiary.api.repository.AccountTokenRepository;
import com.mycropdiary.api.repository.AppUserRepository;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.PlotRepository;
import com.mycropdiary.api.repository.ProductionAreaRepository;
import com.mycropdiary.api.repository.StaffAreaAssignmentRepository;
import com.mycropdiary.api.service.AreaAssignmentService;
import com.mycropdiary.api.service.FarmStaffService;
import com.mycropdiary.api.service.PlotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class FarmCoreAndStaffServiceTest {

    @Autowired
    private PlotService plotService;

    @Autowired
    private FarmStaffService farmStaffService;

    @Autowired
    private AreaAssignmentService areaAssignmentService;

    @Autowired
    private AccountTokenRepository tokenRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private FarmMemberRepository farmMemberRepository;

    @Autowired
    private ProductionAreaRepository areaRepository;

    @Autowired
    private StaffAreaAssignmentRepository assignmentRepository;

    @Autowired
    private PlotRepository plotRepository;

    private AppUser ownerUser;
    private AppUser staffUser;
    private AppUser outsiderUser;
    private Farm farm;
    private FarmMember ownerMember;
    private FarmMember staffMember;
    private ProductionArea area1;
    private ProductionArea area2;

    @BeforeEach
    void setUp() {
        assignmentRepository.deleteAll();
        plotRepository.deleteAll();
        farmMemberRepository.deleteAll();
        areaRepository.deleteAll();
        farmRepository.deleteAll();
        tokenRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Tạo users
        ownerUser = userRepository.save(new AppUser("owner@farm.com", "hash", "Chu Trang Trai", "0901234567", "USER", "ACTIVE"));
        staffUser = userRepository.save(new AppUser("staff@farm.com", "hash", "Nhan Vien A", "0907654321", "USER", "ACTIVE"));
        outsiderUser = userRepository.save(new AppUser("outsider@farm.com", "hash", "Nguoi Ngoai", "0911223344", "USER", "ACTIVE"));

        // 2. Tạo farm
        farm = farmRepository.save(new Farm("FARM01", "Trang Trai Xanh"));

        // 3. Tạo farm members
        ownerMember = farmMemberRepository.save(new FarmMember(farm, ownerUser, "OWNER", "Chu trang trai", "ACTIVE"));
        staffMember = farmMemberRepository.save(new FarmMember(farm, staffUser, "STAFF", "Ky thuat vien", "ACTIVE"));

        // 4. Tạo 2 khu vực sản xuất
        area1 = areaRepository.save(new ProductionArea(farm, "KV-01", "Khu Vuc Rau", new BigDecimal("10000.00"), "Khu vuc rau sach", "ACTIVE"));
        area2 = areaRepository.save(new ProductionArea(farm, "KV-02", "Khu Vuc Cay An Qua", new BigDecimal("20000.00"), "Khu vuc cay an trai", "ACTIVE"));
    }

    // ==========================================
    // UC-33: Quản lý nhân viên trang trại
    // ==========================================

    @Test
    @DisplayName("UC-33: Owner xem danh sách nhân viên thành công")
    void testGetStaffList_Success() {
        List<FarmMemberResponse> members = farmStaffService.getStaffList(ownerUser.getId(), farm.getId(), null, null);
        assertEquals(2, members.size());
    }

    @Test
    @DisplayName("UC-33: Owner thêm nhân viên mới thành công")
    void testAddStaff_Success() {
        AddStaffRequest request = new AddStaffRequest("outsider@farm.com", null, "STAFF", "Nhan vien thu hoach");
        FarmMemberResponse response = farmStaffService.addStaff(ownerUser.getId(), farm.getId(), request);

        assertNotNull(response.id());
        assertEquals("outsider@farm.com", response.email());
        assertEquals("STAFF", response.farmRole());
    }

    @Test
    @DisplayName("UC-33: Thêm nhân viên đã active -> Ném lỗi ResourceConflictException")
    void testAddStaff_AlreadyActive_ThrowsConflict() {
        AddStaffRequest request = new AddStaffRequest("staff@farm.com", null, "STAFF", "Ky thuat");
        assertThrows(ResourceConflictException.class, () -> 
                farmStaffService.addStaff(ownerUser.getId(), farm.getId(), request));
    }

    @Test
    @DisplayName("UC-33: Nhân viên (STAFF) cố tình thêm nhân viên -> Chặn AccessDeniedException")
    void testAddStaff_ByStaff_ThrowsAccessDenied() {
        AddStaffRequest request = new AddStaffRequest("outsider@farm.com", null, "STAFF", "Nhan vien");
        assertThrows(AccessDeniedException.class, () -> 
                farmStaffService.addStaff(staffUser.getId(), farm.getId(), request));
    }

    @Test
    @DisplayName("UC-33: Owner ngừng hoạt động nhân viên -> Tự động đóng tất cả phân công khu vực")
    void testDeactivateStaff_ClosesAssignments() {
        // Phân công staff vào area1
        CreateAssignmentRequest assignReq = new CreateAssignmentRequest(staffMember.getId(), area1.getId(), LocalDate.now(), null);
        areaAssignmentService.assignStaffToArea(ownerUser.getId(), farm.getId(), assignReq);

        // Ngừng hoạt động staff
        farmStaffService.deactivateStaff(ownerUser.getId(), farm.getId(), staffMember.getId());

        // Kiểm tra phân công đã bị vô hiệu hóa
        boolean hasActive = assignmentRepository.existsByFarmMemberIdAndProductionAreaIdAndIsActiveTrue(staffMember.getId(), area1.getId());
        assertFalse(hasActive);
    }

    // ==========================================
    // UC-34 / UC-14: Phân công và xem khu vực được giao
    // ==========================================

    @Test
    @DisplayName("UC-34: Owner phân công nhân viên vào khu vực thành công")
    void testAssignStaffToArea_Success() {
        CreateAssignmentRequest request = new CreateAssignmentRequest(staffMember.getId(), area1.getId(), LocalDate.now(), null);
        AssignmentResponse response = areaAssignmentService.assignStaffToArea(ownerUser.getId(), farm.getId(), request);

        assertNotNull(response.id());
        assertEquals("KV-01", response.areaCode());
        assertEquals(staffMember.getId(), response.farmMemberId());
        assertTrue(response.isActive());
    }

    @Test
    @DisplayName("UC-34: Phân công trùng lặp nhân viên vào khu vực đang active -> Ném lỗi ResourceConflictException")
    void testAssignStaffToArea_Duplicate_ThrowsConflict() {
        CreateAssignmentRequest request = new CreateAssignmentRequest(staffMember.getId(), area1.getId(), LocalDate.now(), null);
        areaAssignmentService.assignStaffToArea(ownerUser.getId(), farm.getId(), request);

        assertThrows(ResourceConflictException.class, () -> 
                areaAssignmentService.assignStaffToArea(ownerUser.getId(), farm.getId(), request));
    }

    @Test
    @DisplayName("UC-14: Nhân viên xem khu vực được giao -> Chỉ trả đúng khu vực được phân công")
    void testGetAssignedAreasForCurrentStaff() {
        // Gán staff vào duy nhất area1
        CreateAssignmentRequest request = new CreateAssignmentRequest(staffMember.getId(), area1.getId(), LocalDate.now(), null);
        areaAssignmentService.assignStaffToArea(ownerUser.getId(), farm.getId(), request);

        // Staff gọi API xem khu vực được giao
        List<AssignedAreaResponse> assignedAreas = areaAssignmentService.getAssignedAreasForCurrentStaff(staffUser.getId(), farm.getId());
        assertEquals(1, assignedAreas.size());
        assertEquals("KV-01", assignedAreas.get(0).areaCode());
    }

    // ==========================================
    // UC-17: Quản lý thửa đất (Plot)
    // ==========================================

    @Test
    @DisplayName("UC-17: Tạo thửa đất thành công khi có quyền phụ trách khu vực")
    void testCreatePlot_Success() {
        CreatePlotRequest request = new CreatePlotRequest(
                "THUA-01",
                "Thua Dat 1",
                new BigDecimal("1500.00"),
                new BigDecimal("10.762622"),
                new BigDecimal("106.660172"),
                "{\"type\":\"Polygon\"}",
                "AVAILABLE"
        );

        PlotResponse response = plotService.createPlot(ownerUser.getId(), area1.getId(), request);
        assertNotNull(response.id());
        assertEquals("THUA-01", response.plotCode());
        assertEquals(new BigDecimal("1500.00"), response.areaM2());
        assertEquals("AVAILABLE", response.status());
    }

    @Test
    @DisplayName("UC-17: Tạo thửa đất trùng mã trong cùng khu vực -> Ném lỗi ResourceConflictException")
    void testCreatePlot_DuplicateCode_ThrowsConflict() {
        CreatePlotRequest request1 = new CreatePlotRequest("THUA-01", "Thua 1", new BigDecimal("1000.00"), null, null, null, "AVAILABLE");
        CreatePlotRequest request2 = new CreatePlotRequest("THUA-01", "Thua 1 trung", new BigDecimal("500.00"), null, null, null, "AVAILABLE");

        plotService.createPlot(ownerUser.getId(), area1.getId(), request1);

        assertThrows(ResourceConflictException.class, () -> 
                plotService.createPlot(ownerUser.getId(), area1.getId(), request2));
    }

    @Test
    @DisplayName("UC-17: Nhân viên chưa được phân công khu vực tạo thửa đất -> Chặn AccessDeniedException")
    void testCreatePlot_UnassignedStaff_ThrowsAccessDenied() {
        CreatePlotRequest request = new CreatePlotRequest("THUA-X", "Thua X", new BigDecimal("500.00"), null, null, null, "AVAILABLE");

        // staffUser chưa được gán area2 -> phải bị chặn
        assertThrows(AccessDeniedException.class, () -> 
                plotService.createPlot(staffUser.getId(), area2.getId(), request));
    }

    @Test
    @DisplayName("UC-17: Tạo thửa đất vượt quá diện tích tối đa của khu vực -> Ném lỗi BusinessRuleException")
    void testCreatePlot_ExceedsAreaCapacity_ThrowsBusinessRule() {
        // area1 diện tích 10,000 m2 -> tạo thửa 15,000 m2 phải bị chặn
        CreatePlotRequest request = new CreatePlotRequest("THUA-BIG", "Thua Sieu To", new BigDecimal("15000.00"), null, null, null, "AVAILABLE");

        assertThrows(BusinessRuleException.class, () -> 
                plotService.createPlot(ownerUser.getId(), area1.getId(), request));
    }
}
