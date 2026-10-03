package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.cultivation.CreateMaterialUsageRequest;
import com.mycropdiary.api.dto.cultivation.MaterialUsageResponse;
import com.mycropdiary.api.entity.AppUser;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.Plot;
import com.mycropdiary.api.entity.ProductionArea;
import com.mycropdiary.api.entity.cultivation.*;

import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ForbiddenException;

import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.StaffAreaAssignmentRepository;
import com.mycropdiary.api.repository.cultivation.*;

import com.mycropdiary.api.service.impl.MaterialUsageServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaterialUsageServiceTest {

    @Mock
    private MaterialUsageRepository materialUsageRepository;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private InputPurchaseDetailRepository inputPurchaseDetailRepository;
    @Mock
    private FarmingActivityRepository farmingActivityRepository;
    @Mock
    private FarmMemberRepository farmMemberRepository;
    @Mock
    private StaffAreaAssignmentRepository staffAreaAssignmentRepository;

    @InjectMocks
    private MaterialUsageServiceImpl materialUsageService;

    private Farm farm;
    private ProductionArea areaA;
    private ProductionArea areaB;
    private Plot plotA;
    private Plot plotB;
    private CropSeason activeSeason;
    private CropSeason completedSeason;
    private FarmingActivity activity;
    private AppUser ownerUser;
    private AppUser staffUser;
    private FarmMember ownerMember;
    private FarmMember staffMember;
    private Material fertilizerMaterial;
    private Material pesticideMaterial;
    private Material inactiveMaterial;

    @BeforeEach
    void setUp() {
        farm = new Farm();
        farm.setId(1L);
        farm.setFarmName("Nông trại Việt");

        areaA = new ProductionArea();
        areaA.setId(10L);
        areaA.setFarm(farm);

        areaB = new ProductionArea();
        areaB.setId(20L);
        areaB.setFarm(farm);

        plotA = new Plot();
        plotA.setId(100L);
        plotA.setProductionArea(areaA);

        plotB = new Plot();
        plotB.setId(200L);
        plotB.setProductionArea(areaB);

        ownerUser = new AppUser();
        ownerUser.setId(1L);
        ownerUser.setFullName("Chủ Nông Trại");

        staffUser = new AppUser();
        staffUser.setId(2L);
        staffUser.setFullName("Nhân Viên Trại");

        ownerMember = new FarmMember();
        ownerMember.setId(101L);
        ownerMember.setFarm(farm);
        ownerMember.setUser(ownerUser);
        ownerMember.setFarmRole("OWNER");
        ownerMember.setStatus("ACTIVE");

        staffMember = new FarmMember();
        staffMember.setId(102L);
        staffMember.setFarm(farm);
        staffMember.setUser(staffUser);
        staffMember.setFarmRole("STAFF");
        staffMember.setStatus("ACTIVE");

        activeSeason = new CropSeason();
        activeSeason.setId(50L);
        activeSeason.setFarm(farm);
        activeSeason.setPlot(plotA);
        activeSeason.setStatus("ACTIVE");

        completedSeason = new CropSeason();
        completedSeason.setId(51L);
        completedSeason.setFarm(farm);
        completedSeason.setPlot(plotA);
        completedSeason.setStatus("COMPLETED");

        activity = new FarmingActivity();
        activity.setId(500L);
        activity.setCropSeason(activeSeason);
        activity.setSupervisedByMember(ownerMember);
        activity.setActivityName("Bón phân đợt 1");

        fertilizerMaterial = new Material(1L, farm, "FERT01", "Phân NPK 16-16-8", MaterialType.FERTILIZER, "kg", true);
        pesticideMaterial = new Material(2L, farm, "PEST01", "Thuốc Trừ Sâu An Toàn", MaterialType.PESTICIDE, "chai", true);
        inactiveMaterial = new Material(3L, farm, "OLD01", "Vật tư cũ", MaterialType.OTHER, "cái", false);
    }

    @Test
    @DisplayName("Tạo MaterialUsage thành công với vai trò OWNER")
    void createMaterialUsage_AsOwner_Success() {
        CreateMaterialUsageRequest request = new CreateMaterialUsageRequest(
                500L, 1L, null, LocalDateTime.now(), new BigDecimal("2.500"), "kg", "50g/gốc", "Rải gốc", null, "Thời tiết nắng"
        );

        when(farmingActivityRepository.findById(500L)).thenReturn(Optional.of(activity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1L, 1L, "ACTIVE")).thenReturn(Optional.of(ownerMember));
        when(materialRepository.findById(1L)).thenReturn(Optional.of(fertilizerMaterial));

        MaterialUsage savedUsage = new MaterialUsage();
        savedUsage.setId(99L);
        savedUsage.setFarmingActivity(activity);
        savedUsage.setMaterial(fertilizerMaterial);
        savedUsage.setRecordedByMember(ownerMember);
        savedUsage.setUsedAt(request.usedAt());
        savedUsage.setQuantity(request.quantity());
        savedUsage.setUnit("kg");

        when(materialUsageRepository.save(any(MaterialUsage.class))).thenReturn(savedUsage);

        MaterialUsageResponse response = materialUsageService.createMaterialUsage(1L, request);

        assertNotNull(response);
        assertEquals(99L, response.materialUsageId());
        assertEquals("Phân NPK 16-16-8", response.materialName());
        assertEquals("FERTILIZER", response.materialType());
        verify(materialUsageRepository).save(any(MaterialUsage.class));
    }

    @Test
    @DisplayName("Tạo MaterialUsage thành công với STAFF được assign đúng ProductionArea")
    void createMaterialUsage_AsStaff_AssignedToArea_Success() {
        CreateMaterialUsageRequest request = new CreateMaterialUsageRequest(
                500L, 1L, null, LocalDateTime.now(), new BigDecimal("1.000"), "kg", null, null, null, null
        );

        when(farmingActivityRepository.findById(500L)).thenReturn(Optional.of(activity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1L, 2L, "ACTIVE")).thenReturn(Optional.of(staffMember));
        when(staffAreaAssignmentRepository.existsByFarmMemberIdAndProductionAreaIdAndActiveTrue(102L, 10L)).thenReturn(true);
        when(materialRepository.findById(1L)).thenReturn(Optional.of(fertilizerMaterial));

        MaterialUsage savedUsage = new MaterialUsage();
        savedUsage.setId(100L);
        savedUsage.setFarmingActivity(activity);
        savedUsage.setMaterial(fertilizerMaterial);
        savedUsage.setRecordedByMember(staffMember);

        when(materialUsageRepository.save(any(MaterialUsage.class))).thenReturn(savedUsage);

        MaterialUsageResponse response = materialUsageService.createMaterialUsage(2L, request);

        assertNotNull(response);
        assertEquals(100L, response.materialUsageId());
    }

    @Test
    @DisplayName("Tạo MaterialUsage thất bại nếu STAFF không được assign ProductionArea")
    void createMaterialUsage_AsStaff_NotAssigned_ThrowsForbidden() {
        CreateMaterialUsageRequest request = new CreateMaterialUsageRequest(
                500L, 1L, null, LocalDateTime.now(), new BigDecimal("1.000"), "kg", null, null, null, null
        );

        when(farmingActivityRepository.findById(500L)).thenReturn(Optional.of(activity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1L, 2L, "ACTIVE")).thenReturn(Optional.of(staffMember));
        when(staffAreaAssignmentRepository.existsByFarmMemberIdAndProductionAreaIdAndActiveTrue(102L, 10L)).thenReturn(false);

        ForbiddenException exception = assertThrows(ForbiddenException.class,
                () -> materialUsageService.createMaterialUsage(2L, request));

        assertTrue(exception.getMessage().contains("Nhân viên chưa được phân công"));
        verify(materialUsageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tạo MaterialUsage thất bại khi Unit trong request không khớp với Material.unit (kg vs L)")
    void createMaterialUsage_UnitMismatch_ThrowsBadRequest() {
        // Material.unit = "kg", Request.unit = "L"
        CreateMaterialUsageRequest request = new CreateMaterialUsageRequest(
                500L, 1L, null, LocalDateTime.now(), new BigDecimal("1.000"), "L", null, null, null, null
        );

        when(farmingActivityRepository.findById(500L)).thenReturn(Optional.of(activity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1L, 1L, "ACTIVE")).thenReturn(Optional.of(ownerMember));
        when(materialRepository.findById(1L)).thenReturn(Optional.of(fertilizerMaterial)); // Unit là "kg"

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> materialUsageService.createMaterialUsage(1L, request));

        assertTrue(exception.getMessage().contains("không khớp với đơn vị tính của vật tư"));
    }

    @Test
    @DisplayName("Tạo MaterialUsage thất bại nếu Material thuộc Farm khác")
    void createMaterialUsage_MaterialBelongsToDifferentFarm_ThrowsBadRequest() {
        Farm farmB = new Farm();
        farmB.setId(2L);
        Material otherFarmMaterial = new Material(5L, farmB, "OTHER", "Phân Farm B", MaterialType.FERTILIZER, "kg", true);

        CreateMaterialUsageRequest request = new CreateMaterialUsageRequest(
                500L, 5L, null, LocalDateTime.now(), new BigDecimal("1.000"), "kg", null, null, null, null
        );

        when(farmingActivityRepository.findById(500L)).thenReturn(Optional.of(activity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1L, 1L, "ACTIVE")).thenReturn(Optional.of(ownerMember));
        when(materialRepository.findById(5L)).thenReturn(Optional.of(otherFarmMaterial));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> materialUsageService.createMaterialUsage(1L, request));

        assertTrue(exception.getMessage().contains("không thuộc trang trại"));
    }

    @Test
    @DisplayName("Tạo MaterialUsage thất bại nếu Material inactive")
    void createMaterialUsage_MaterialInactive_ThrowsBadRequest() {
        CreateMaterialUsageRequest request = new CreateMaterialUsageRequest(
                500L, 3L, null, LocalDateTime.now(), new BigDecimal("1.000"), "cái", null, null, null, null
        );

        when(farmingActivityRepository.findById(500L)).thenReturn(Optional.of(activity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1L, 1L, "ACTIVE")).thenReturn(Optional.of(ownerMember));
        when(materialRepository.findById(3L)).thenReturn(Optional.of(inactiveMaterial));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> materialUsageService.createMaterialUsage(1L, request));

        assertTrue(exception.getMessage().contains("vô hiệu hóa"));
    }

    @Test
    @DisplayName("Tạo MaterialUsage thất bại nếu Quantity <= 0")
    void createMaterialUsage_QuantityInvalid_ThrowsBadRequest() {
        CreateMaterialUsageRequest request = new CreateMaterialUsageRequest(
                500L, 1L, null, LocalDateTime.now(), new BigDecimal("0.000"), "kg", null, null, null, null
        );

        when(farmingActivityRepository.findById(500L)).thenReturn(Optional.of(activity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1L, 1L, "ACTIVE")).thenReturn(Optional.of(ownerMember));
        when(materialRepository.findById(1L)).thenReturn(Optional.of(fertilizerMaterial));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> materialUsageService.createMaterialUsage(1L, request));

        assertTrue(exception.getMessage().contains("phải lớn hơn 0"));
    }

    @Test
    @DisplayName("Tạo MaterialUsage thất bại nếu PESTICIDE thiếu SafetyIntervalDays")
    void createMaterialUsage_PesticideMissingSafetyInterval_ThrowsBadRequest() {
        CreateMaterialUsageRequest request = new CreateMaterialUsageRequest(
                500L, 2L, null, LocalDateTime.now(), new BigDecimal("1.000"), "chai", null, null, null, null
        );

        when(farmingActivityRepository.findById(500L)).thenReturn(Optional.of(activity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1L, 1L, "ACTIVE")).thenReturn(Optional.of(ownerMember));
        when(materialRepository.findById(2L)).thenReturn(Optional.of(pesticideMaterial));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> materialUsageService.createMaterialUsage(1L, request));

        assertTrue(exception.getMessage().contains("SafetyIntervalDays"));
    }

    @Test
    @DisplayName("Tạo MaterialUsage thành công với PESTICIDE khi truyền SafetyIntervalDays hợp lệ")
    void createMaterialUsage_PesticideWithSafetyInterval_Success() {
        CreateMaterialUsageRequest request = new CreateMaterialUsageRequest(
                500L, 2L, null, LocalDateTime.now(), new BigDecimal("1.000"), "chai", "10ml/bình", "Phun lá", 14, "Cách ly 14 ngày"
        );

        when(farmingActivityRepository.findById(500L)).thenReturn(Optional.of(activity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1L, 1L, "ACTIVE")).thenReturn(Optional.of(ownerMember));
        when(materialRepository.findById(2L)).thenReturn(Optional.of(pesticideMaterial));

        MaterialUsage savedUsage = new MaterialUsage();
        savedUsage.setId(101L);
        savedUsage.setFarmingActivity(activity);
        savedUsage.setMaterial(pesticideMaterial);
        savedUsage.setRecordedByMember(ownerMember);
        savedUsage.setSafetyIntervalDays(14);

        when(materialUsageRepository.save(any(MaterialUsage.class))).thenReturn(savedUsage);

        MaterialUsageResponse response = materialUsageService.createMaterialUsage(1L, request);

        assertNotNull(response);
        assertEquals(14, response.safetyIntervalDays());
    }

    @Test
    @DisplayName("Tạo MaterialUsage thất bại khi CropSeason đã COMPLETED")
    void createMaterialUsage_ClosedSeason_ThrowsBadRequest() {
        FarmingActivity closedActivity = new FarmingActivity();
        closedActivity.setId(600L);
        closedActivity.setCropSeason(completedSeason);

        CreateMaterialUsageRequest request = new CreateMaterialUsageRequest(
                600L, 1L, null, LocalDateTime.now(), new BigDecimal("1.000"), "kg", null, null, null, null
        );

        when(farmingActivityRepository.findById(600L)).thenReturn(Optional.of(closedActivity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1L, 1L, "ACTIVE")).thenReturn(Optional.of(ownerMember));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> materialUsageService.createMaterialUsage(1L, request));

        assertTrue(exception.getMessage().contains("mùa vụ đã đóng"));
    }

    @Test
    @DisplayName("Xem danh sách MaterialUsage theo Activity có phân trang thành công")
    void getMaterialUsagesByActivity_Success() {
        MaterialUsage usage = new MaterialUsage();
        usage.setId(1L);
        usage.setFarmingActivity(activity);
        usage.setMaterial(fertilizerMaterial);
        usage.setRecordedByMember(ownerMember);

        Pageable pageable = PageRequest.of(0, 20);

        when(farmingActivityRepository.findById(500L)).thenReturn(Optional.of(activity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1L, 1L, "ACTIVE")).thenReturn(Optional.of(ownerMember));
        when(materialUsageRepository.findByFarmingActivityId(500L, pageable)).thenReturn(new PageImpl<>(List.of(usage)));

        PageResponse<MaterialUsageResponse> result = materialUsageService.getMaterialUsagesByActivity(1L, 500L, pageable);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals(1L, result.content().get(0).materialUsageId());
    }
}
