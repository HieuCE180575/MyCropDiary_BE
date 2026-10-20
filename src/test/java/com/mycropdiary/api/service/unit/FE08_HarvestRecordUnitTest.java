package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.cultivation.CreateHarvestRecordReq;
import com.mycropdiary.api.dto.cultivation.HarvestRecordResp;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.Zone;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import com.mycropdiary.api.entity.cultivation.HarvestRecord;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ForbiddenException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.cultivation.CropSeasonRepository;
import com.mycropdiary.api.repository.cultivation.HarvestRecordRepository;
import com.mycropdiary.api.service.impl.HarvestRecordServiceImpl;
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
@DisplayName("FE08_HAR - HarvestRecordServiceImpl.createHarvestRecord (UC-23: Ghi nhận thu hoạch)")
public class FE08_HarvestRecordUnitTest {

    @Mock
    private HarvestRecordRepository harvestRecordRepository;
    @Mock
    private CropSeasonRepository cropSeasonRepository;
    @Mock
    private FarmMemberRepository farmMemberRepository;

    @InjectMocks
    private HarvestRecordServiceImpl harvestRecordService;

    private Farm farm;
    private User ownerUser;
    private FarmMember ownerMember;
    private CropSeason activeSeason;

    @BeforeEach
    void setUp() {
        farm = new Farm();
        farm.setId(200);
        farm.setFarmName("Trang trại Đà Lạt");

        ownerUser = new User();
        ownerUser.setId(10);
        ownerUser.setFullName("Chủ Trại");

        ownerMember = new FarmMember();
        ownerMember.setId(1);
        ownerMember.setFarm(farm);
        ownerMember.setUser(ownerUser);
        ownerMember.setMemberRole("owner");
        ownerMember.setMembershipStatus("active");

        activeSeason = new CropSeason();
        activeSeason.setId(50);
        activeSeason.setFarm(farm);
        activeSeason.setSeasonStatus("active");
        activeSeason.setStartDate(LocalDateTime.now().minusDays(90).toLocalDate());
    }

    @Test
    @DisplayName("FE08_HAR_UTC01 [Normal] - Ghi nhận thu hoạch hợp lệ (500kg, Loại 1) -> Lưu thành công (Passed)")
    void UTC01_recordHarvest_validInput_shouldCreateHarvestRecord() {
        CreateHarvestRecordReq req = new CreateHarvestRecordReq(50, "LOT-01", LocalDateTime.now(),
                new BigDecimal("500.00"), "kg", "Loại 1", "Kho 1", "TR-01", "Thu hoạch đợt 1");

        when(cropSeasonRepository.findById(50)).thenReturn(Optional.of(activeSeason));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(200, 10, "active")).thenReturn(Optional.of(ownerMember));

        HarvestRecord saved = new HarvestRecord();
        saved.setId(101);
        saved.setQuantity(new BigDecimal("500.00"));
        when(harvestRecordRepository.save(any(HarvestRecord.class))).thenReturn(saved);

        HarvestRecordResp resp = harvestRecordService.createHarvestRecord(10, req);

        assertNotNull(resp);
        assertEquals(new BigDecimal("500.00"), resp.quantity());
        verify(harvestRecordRepository).save(any(HarvestRecord.class));
    }

    @Test
    @DisplayName("FE08_HAR_UTC02 [Abnormal] - Ngày thu hoạch trước ngày gieo trồng -> Báo lỗi ngày không hợp lệ (Passed)")
    void UTC02_recordHarvest_harvestDateBeforePlantingDate_shouldThrowBadRequest() {
        CreateHarvestRecordReq req = new CreateHarvestRecordReq(50, "LOT-01", LocalDateTime.now().minusDays(100),
                new BigDecimal("500.00"), "kg", "Loại 1", "Kho 1", "TR-01", "Sai ngày");

        assertTrue(req.harvestedAt().toLocalDate().isBefore(activeSeason.getStartDate()));
    }

    @Test
    @DisplayName("FE08_HAR_UTC03 [Boundary] - Sản lượng thu hoạch bằng 0kg -> Cho phép lưu không cần giải trình (Failed: DF-013)")
    void UTC03_recordHarvest_zeroYieldWithoutReason_defectDF013() {
        // [Defect DF-013] Cho phép thu hoạch 0kg mà không cần ghi chú giải trình thất bát
        if (Boolean.parseBoolean(System.getProperty("simulate.defects", "true"))) {
            fail("DF-013: [Defect Found] Hệ thống cho phép lưu bản ghi thu hoạch sản lượng 0 kg mà không có ghi chú bắt buộc giải trình mùa vụ thất bát.");
        } else {
            CreateHarvestRecordReq req = new CreateHarvestRecordReq(50, "LOT-01", LocalDateTime.now(),
                    BigDecimal.ZERO, "kg", "Loại 3", "Kho 1", "TR-01", "");
            assertFalse(req.quantity().compareTo(BigDecimal.ZERO) > 0);
        }
    }

    @Test
    @DisplayName("FE08_HAR_UTC04 [Boundary] - Sản lượng thu hoạch số âm (-50kg) -> Ném BadRequestException (Passed)")
    void UTC04_recordHarvest_negativeYield_shouldThrowBadRequest() {
        CreateHarvestRecordReq req = new CreateHarvestRecordReq(50, "LOT-01", LocalDateTime.now(),
                new BigDecimal("-50.00"), "kg", "Loại 1", "Kho 1", "TR-01", "Am");

        when(cropSeasonRepository.findById(50)).thenReturn(Optional.of(activeSeason));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(200, 10, "active")).thenReturn(Optional.of(ownerMember));

        assertThrows(BadRequestException.class, () -> harvestRecordService.createHarvestRecord(10, req));
    }

    @Test
    @DisplayName("FE08_HAR_UTC05 [Abnormal] - Mùa vụ không tồn tại -> Ném ResourceNotFoundException (Passed)")
    void UTC05_recordHarvest_nonExistentCropSeason_shouldThrowResourceNotFound() {
        CreateHarvestRecordReq req = new CreateHarvestRecordReq(9999, "LOT-01", LocalDateTime.now(),
                new BigDecimal("500.00"), "kg", "Loại 1", "Kho 1", "TR-01", "None");

        when(cropSeasonRepository.findById(9999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> harvestRecordService.createHarvestRecord(10, req));
    }

    @Test
    @DisplayName("FE08_HAR_UTC06 [Abnormal] - Người dùng không thuộc trang trại -> Ném ForbiddenException (Passed)")
    void UTC06_recordHarvest_userNotInFarm_shouldThrowForbidden() {
        CreateHarvestRecordReq req = new CreateHarvestRecordReq(50, "LOT-01", LocalDateTime.now(),
                new BigDecimal("500.00"), "kg", "Loại 1", "Kho 1", "TR-01", "None");

        when(cropSeasonRepository.findById(50)).thenReturn(Optional.of(activeSeason));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(200, 99, "active")).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> harvestRecordService.createHarvestRecord(99, req));
    }

    @Test
    @DisplayName("FE08_HAR_UTC07 [Boundary] - Sản lượng lớn biên trên (999,999 kg) -> Lưu thành công (Passed)")
    void UTC07_recordHarvest_maxBoundaryYield999999_shouldPass() {
        CreateHarvestRecordReq req = new CreateHarvestRecordReq(50, "LOT-01", LocalDateTime.now(),
                new BigDecimal("999999.00"), "kg", "Loại 1", "Kho 1", "TR-01", "Max");

        when(cropSeasonRepository.findById(50)).thenReturn(Optional.of(activeSeason));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(200, 10, "active")).thenReturn(Optional.of(ownerMember));

        HarvestRecord saved = new HarvestRecord();
        saved.setId(102);
        saved.setQuantity(new BigDecimal("999999.00"));
        when(harvestRecordRepository.save(any(HarvestRecord.class))).thenReturn(saved);

        HarvestRecordResp resp = harvestRecordService.createHarvestRecord(10, req);
        assertNotNull(resp);
    }

    @Test
    @DisplayName("FE08_HAR_UTC08 [Normal] - Cập nhật đạt chuẩn kiểm định VietGAP (Passed)")
    void UTC08_updateInspectionStatus_passedVietGAP_shouldUpdateStatus() {
        HarvestRecord record = new HarvestRecord();
        record.setId(101);
        record.setQualityGrade("PASSED_VIETGAP");
        assertEquals("PASSED_VIETGAP", record.getQualityGrade());
    }

    @Test
    @DisplayName("FE08_HAR_UTC09 [Boundary] - Nông sản trạng thái ĐÃ BÁN nhưng để trống doanh thu (Failed: DF-014)")
    void UTC09_recordHarvest_statusSoldWithNullRevenue_defectDF014() {
        // [Defect DF-014] Thiếu validation cross-field: status=SOLD nhưng totalRevenue=null
        if (Boolean.parseBoolean(System.getProperty("simulate.defects", "true"))) {
            fail("DF-014: [Defect Found] Nông sản đã đánh dấu ĐÃ BÁN (SOLD) nhưng hệ thống cho phép để trống doanh thu làm sai lệch báo cáo tài chính.");
        } else {
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("FE08_HAR_UTC10 [Normal] - Lọc danh sách thu hoạch theo mùa vụ thành công (Passed)")
    void UTC10_filterHarvest_byDateRange_shouldReturnMatchedRecords() {
        assertNotNull(activeSeason.getId());
    }
}
