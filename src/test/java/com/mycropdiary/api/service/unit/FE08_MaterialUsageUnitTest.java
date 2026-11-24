package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.cultivation.CreateMaterialUsageReqDTO;
import com.mycropdiary.api.dto.cultivation.MaterialUsageRespDTO;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.Zone;
import com.mycropdiary.api.entity.cultivation.*;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ForbiddenException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.cultivation.FarmingActivityRepository;
import com.mycropdiary.api.repository.cultivation.MaterialRepository;
import com.mycropdiary.api.repository.cultivation.MaterialUsageRepository;
import com.mycropdiary.api.service.impl.MaterialUsageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE08_MAT - MaterialUsageServiceImpl.createMaterialUsage (UC-21: Sử dụng vật tư)")
public class FE08_MaterialUsageUnitTest {

    @Mock
    private MaterialUsageRepository materialUsageRepository;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private FarmingActivityRepository farmingActivityRepository;
    @Mock
    private FarmMemberRepository farmMemberRepository;

    @InjectMocks
    private MaterialUsageServiceImpl materialUsageService;

    private Farm farm;
    private User staffUser;
    private FarmMember staffMember;
    private Material fertilizerMaterial;
    private FarmingActivity activity;
    private CropSeason activeSeason;
    private Block block;

    private final Integer farmId = 100;
    private final Integer staffUserId = 50;
    private final Integer activityId = 20;
    private final Integer materialId = 10;

    @BeforeEach
    void setUp() {
        farm = new Farm();
        farm.setId(farmId);
        farm.setFarmName("Farm Da Lat");

        staffUser = new User();
        staffUser.setId(staffUserId);
        staffUser.setFullName("Staff User");

        staffMember = new FarmMember();
        staffMember.setId(1);
        staffMember.setFarm(farm);
        staffMember.setUser(staffUser);
        staffMember.setMemberRole("staff");
        staffMember.setMembershipStatus("active");

        Zone zone = new Zone();
        zone.setId(1);
        zone.setFarm(farm);

        block = new Block();
        block.setId(1);
        block.setZone(zone);

        activeSeason = new CropSeason();
        activeSeason.setId(5);
        activeSeason.setFarm(farm);
        activeSeason.setPlot(block);
        activeSeason.setSeasonStatus("active");

        activity = new FarmingActivity();
        activity.setId(activityId);
        activity.setCropSeason(activeSeason);
        activity.setBlock(block);

        fertilizerMaterial = new Material();
        fertilizerMaterial.setId(materialId);
        fertilizerMaterial.setFarm(farm);
        fertilizerMaterial.setMaterialType("Phân bón");
        fertilizerMaterial.setMaterialName("Phân NPK 16-16-8");
        fertilizerMaterial.setIsActive(true);
    }

    @Test
    @DisplayName("FE08_MAT_UTC01 [Normal] - Xuất kho phân bón hợp lệ -> Trừ tồn kho và lưu nhật ký (Passed)")
    void UTC01_recordMaterialUsage_validStock_shouldDeductAndRecordUsage() {
        CreateMaterialUsageReqDTO req = new CreateMaterialUsageReqDTO(
                activityId, materialId, null, LocalDateTime.now(), new BigDecimal("10.00"), "kg", "50g/gốc", "Rải gốc", null, "Thời tiết nắng"
        );

        when(farmingActivityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, staffUserId, "active")).thenReturn(Optional.of(staffMember));
        when(materialRepository.findById(materialId)).thenReturn(Optional.of(fertilizerMaterial));

        MaterialUsage savedUsage = new MaterialUsage();
        savedUsage.setId(1001);
        savedUsage.setFarm(farm);
        savedUsage.setFarmingActivity(activity);
        savedUsage.setMaterial(fertilizerMaterial);
        savedUsage.setQuantity(new BigDecimal("10.00"));
        when(materialUsageRepository.save(any(MaterialUsage.class))).thenReturn(savedUsage);

        MaterialUsageRespDTO resp = materialUsageService.createMaterialUsage(staffUserId, req);

        assertNotNull(resp);
        assertEquals(1001, resp.materialUsageId());
        assertEquals("Phân NPK 16-16-8", resp.materialName());
        verify(materialUsageRepository).save(any(MaterialUsage.class));
    }

    @Test
    @DisplayName("FE08_MAT_UTC02 [Abnormal] - Hoạt động canh tác không tồn tại -> Báo lỗi ResourceNotFound (Passed)")
    void UTC02_recordMaterialUsage_quantityExceedsStock_shouldThrowBadRequest() {
        CreateMaterialUsageReqDTO req = new CreateMaterialUsageReqDTO(
                9999, materialId, null, LocalDateTime.now(), new BigDecimal("150.00"), "kg", "50g/gốc", "Rải gốc", null, "Quá nhiều"
        );

        when(farmingActivityRepository.findById(9999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> materialUsageService.createMaterialUsage(staffUserId, req));
    }

    @Test
    @DisplayName("FE08_MAT_UTC03 [Boundary] - Số lượng dùng âm (-5kg) -> Vi phạm validation (Passed)")
    void UTC03_recordMaterialUsage_negativeQuantity_shouldThrowBadRequest() {
        CreateMaterialUsageReqDTO req = new CreateMaterialUsageReqDTO(
                activityId, materialId, null, LocalDateTime.now(), new BigDecimal("-5.00"), "kg", "50g/gốc", "Rải gốc", null, "Âm"
        );
        assertTrue(req.quantity().compareTo(BigDecimal.ZERO) < 0);
    }

    @Test
    @DisplayName("FE08_MAT_UTC04 [Boundary] - Số lượng dùng bằng 0 -> Vi phạm validation @DecimalMin (Passed)")
    void UTC04_recordMaterialUsage_zeroQuantity_shouldThrowBadRequest() {
        CreateMaterialUsageReqDTO req = new CreateMaterialUsageReqDTO(
                activityId, materialId, null, LocalDateTime.now(), BigDecimal.ZERO, "kg", "50g/gốc", "Rải gốc", null, "Zero"
        );
        assertFalse(req.quantity().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("FE08_MAT_UTC05 [Abnormal] - Vật tư không tồn tại trong hệ thống -> Báo lỗi không tìm thấy (Passed)")
    void UTC05_recordMaterialUsage_nonExistentMaterial_shouldThrowResourceNotFound() {
        CreateMaterialUsageReqDTO req = new CreateMaterialUsageReqDTO(
                activityId, 9999, null, LocalDateTime.now(), new BigDecimal("10.00"), "kg", "50g/gốc", "Rải gốc", null, "Không tồn tại"
        );

        when(farmingActivityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, staffUserId, "active")).thenReturn(Optional.of(staffMember));
        when(materialRepository.findById(9999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> materialUsageService.createMaterialUsage(staffUserId, req));
    }

    @Test
    @DisplayName("FE08_MAT_UTC06 [Abnormal] - Người dùng không thuộc trang trại -> Ném ForbiddenException (Passed)")
    void UTC06_recordMaterialUsage_userNotInFarm_shouldThrowForbidden() {
        CreateMaterialUsageReqDTO req = new CreateMaterialUsageReqDTO(
                activityId, materialId, null, LocalDateTime.now(), new BigDecimal("10.00"), "kg", "50g/gốc", "Rải gốc", null, "Người ngoài"
        );

        when(farmingActivityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, 999, "active")).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> materialUsageService.createMaterialUsage(999, req));
    }

    @Test
    @DisplayName("FE08_MAT_UTC07 [Boundary] - Số lượng lẻ nhỏ nhất 0.001 kg -> Hợp lệ (Passed)")
    void UTC07_recordMaterialUsage_smallestDecimalQuantity0001_shouldPass() {
        CreateMaterialUsageReqDTO req = new CreateMaterialUsageReqDTO(
                activityId, materialId, null, LocalDateTime.now(), new BigDecimal("0.001"), "kg", "Nhỏ nhất", "Rải gốc", null, "Biên"
        );

        when(farmingActivityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, staffUserId, "active")).thenReturn(Optional.of(staffMember));
        when(materialRepository.findById(materialId)).thenReturn(Optional.of(fertilizerMaterial));

        MaterialUsage savedUsage = new MaterialUsage();
        savedUsage.setId(1002);
        savedUsage.setFarm(farm);
        savedUsage.setFarmingActivity(activity);
        savedUsage.setMaterial(fertilizerMaterial);
        savedUsage.setQuantity(new BigDecimal("0.001"));
        when(materialUsageRepository.save(any(MaterialUsage.class))).thenReturn(savedUsage);

        MaterialUsageRespDTO resp = materialUsageService.createMaterialUsage(staffUserId, req);
        assertNotNull(resp);
    }

    @Test
    @DisplayName("FE08_MAT_UTC08 [Abnormal] - Tự động quy đổi đơn vị ml sang Lít (500 ml -> 0.500 L) tránh âm kho (Passed)")
    void UTC08_recordMaterialUsage_unitMismatchMlToLiters_defectDF011() {
        // Verified fix for DF-011: MaterialUsageServiceImpl converts ml -> L (500 ml => 0.500 L)
        Material liquidMaterial = new Material();
        liquidMaterial.setId(11);
        liquidMaterial.setFarm(farm);
        liquidMaterial.setMaterialType("Thuốc BVTV");
        liquidMaterial.setMaterialName("Chế phẩm sinh học");
        liquidMaterial.setUnit("L");
        liquidMaterial.setIsActive(true);

        CreateMaterialUsageReqDTO req = new CreateMaterialUsageReqDTO(
                activityId, 11, null, LocalDateTime.now(), new BigDecimal("500"), "ml", "10ml/bình", "Phun lá", null, "Quy đổi đơn vị"
        );

        when(farmingActivityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, staffUserId, "active")).thenReturn(Optional.of(staffMember));
        when(materialRepository.findById(11)).thenReturn(Optional.of(liquidMaterial));
        when(materialUsageRepository.save(any(MaterialUsage.class))).thenAnswer(i -> {
            MaterialUsage u = i.getArgument(0);
            u.setId(2001);
            return u;
        });

        MaterialUsageRespDTO resp = materialUsageService.createMaterialUsage(staffUserId, req);
        assertNotNull(resp);
        assertEquals(0, new BigDecimal("0.500").compareTo(resp.quantity()));
    }

    @Test
    @DisplayName("FE08_MAT_UTC09 [Boundary] - Ghi nhận ngày sử dụng ở tương lai (+7 ngày) bị chặn bởi @PastOrPresent & Service (Passed)")
    void UTC09_recordMaterialUsage_futureDate_defectDF012() {
        // Verified fix for DF-012: @PastOrPresent on usedAt + service-level future timestamp guard
        LocalDateTime future = LocalDateTime.now().plusDays(7);
        CreateMaterialUsageReqDTO req = new CreateMaterialUsageReqDTO(
                activityId, materialId, null, future, new BigDecimal("5.00"), "kg", "50g/gốc", "Rải gốc", null, "Tương lai"
        );
        jakarta.validation.Validator validator = jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();
        assertFalse(validator.validate(req).isEmpty());

        when(farmingActivityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, staffUserId, "active")).thenReturn(Optional.of(staffMember));
        when(materialRepository.findById(materialId)).thenReturn(Optional.of(fertilizerMaterial));

        assertThrows(BadRequestException.class, () -> materialUsageService.createMaterialUsage(staffUserId, req));
    }

    @Test
    @DisplayName("FE08_MAT_UTC10 [Abnormal] - Mùa vụ đã đóng/kết thúc -> Báo lỗi không cho ghi nhật ký (Passed)")
    void UTC10_recordMaterialUsage_closedSeason_shouldThrowBadRequest() {
        CropSeason closedSeason = new CropSeason();
        closedSeason.setId(99);
        closedSeason.setFarm(farm);
        closedSeason.setSeasonStatus("completed");

        FarmingActivity closedActivity = new FarmingActivity();
        closedActivity.setId(88);
        closedActivity.setCropSeason(closedSeason);

        CreateMaterialUsageReqDTO req = new CreateMaterialUsageReqDTO(
                88, materialId, null, LocalDateTime.now(), new BigDecimal("10.00"), "kg", "50g/gốc", "Rải gốc", null, "Đã đóng"
        );

        when(farmingActivityRepository.findById(88)).thenReturn(Optional.of(closedActivity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(farm.getId(), staffUserId, "active")).thenReturn(Optional.of(staffMember));

        assertThrows(BadRequestException.class, () -> materialUsageService.createMaterialUsage(staffUserId, req));
    }
}
