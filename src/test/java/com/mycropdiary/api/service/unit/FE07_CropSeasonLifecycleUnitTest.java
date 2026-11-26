package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.cultivation.CreateCropSeasonReqDTO;
import com.mycropdiary.api.dto.cultivation.CropSeasonRespDTO;
import com.mycropdiary.api.dto.cultivation.UpdateCropSeasonReqDTO;
import com.mycropdiary.api.dto.cultivation.UpdateCropSeasonStatusReqDTO;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.cultivation.CropCategory;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceConflictException;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.repository.cultivation.CropCategoryRepository;
import com.mycropdiary.api.repository.cultivation.CropSeasonRepository;
import com.mycropdiary.api.repository.cultivation.FarmingActivityRepository;
import com.mycropdiary.api.repository.cultivation.HarvestRecordRepository;
import com.mycropdiary.api.service.impl.CropSeasonServiceImpl;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE07_SEA - CropSeasonServiceImpl Lifecycle & Edit/Delete Guard Unit Tests (UC-21)")
public class FE07_CropSeasonLifecycleUnitTest {

    @Mock private CropSeasonRepository cropSeasonRepository;
    @Mock private CropCategoryRepository cropCategoryRepository;
    @Mock private FarmRepository farmRepository;
    @Mock private UserRepository userRepository;
    @Mock private FarmAccessGuard farmAccessGuard;
    @Mock private FarmingActivityRepository farmingActivityRepository;
    @Mock private HarvestRecordRepository harvestRecordRepository;

    @InjectMocks private CropSeasonServiceImpl cropSeasonService;

    private Farm farm;
    private User owner;
    private CropCategory category;
    private CropSeason plannedSeason;

    @BeforeEach
    void setUp() {
        farm = new Farm();
        farm.setId(1);
        farm.setFarmName("Nông trại Sạch");

        owner = new User("owner@farm.vn", "hash", "Chủ Trại", "0901234567", "owner", "active");
        owner.setId(10);

        category = new CropCategory();
        category.setId(5);
        category.setCategoryName("Lúa ST25");

        plannedSeason = new CropSeason();
        plannedSeason.setId(100);
        plannedSeason.setFarm(farm);
        plannedSeason.setCropCategory(category);
        plannedSeason.setSeasonName("Vụ Đông Xuân 2027");
        plannedSeason.setStartDate(LocalDate.now().plusDays(10));
        plannedSeason.setEndDate(LocalDate.now().plusDays(100));
        plannedSeason.setSeasonStatus("planned");
        plannedSeason.setCreatedBy(owner);
    }

    @Test
    @DisplayName("FE07_SEA_UTC01 [Normal] - Tạo mùa vụ có ngày bắt đầu trong tương lai -> Mặc định trạng thái planned")
    void UTC01_createCropSeason_futureStartDate_defaultsToPlanned() {
        CreateCropSeasonReqDTO req = new CreateCropSeasonReqDTO(
                "Vụ Hè Thu 2027", 5, null,
                LocalDate.now().plusDays(15), LocalDate.now().plusDays(110), null, "Ghi chú vụ mới"
        );
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(cropSeasonRepository.existsByFarmIdAndSeasonNameIgnoreCase(1, "Vụ Hè Thu 2027")).thenReturn(false);
        when(cropCategoryRepository.findById(5)).thenReturn(Optional.of(category));
        when(userRepository.findById(10)).thenReturn(Optional.of(owner));
        when(cropSeasonRepository.save(any(CropSeason.class))).thenAnswer(i -> {
            CropSeason s = i.getArgument(0);
            s.setId(101);
            return s;
        });

        CropSeasonRespDTO resp = cropSeasonService.createCropSeason(10, 1, req);

        assertNotNull(resp);
        assertEquals("planned", resp.seasonStatus());
        verify(farmAccessGuard).requireOwnerOrAdmin(10, 1);
    }

    @Test
    @DisplayName("FE07_SEA_UTC02 [Boundary] - Tạo mùa vụ bắt đầu đúng hôm nay (LocalDate.now()) -> Mặc định trạng thái active")
    void UTC02_createCropSeason_todayStartDate_defaultsToActive() {
        CreateCropSeasonReqDTO req = new CreateCropSeasonReqDTO(
                "Vụ Hôm Nay", 5, null,
                LocalDate.now(), LocalDate.now().plusDays(90), null, null
        );
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(cropSeasonRepository.existsByFarmIdAndSeasonNameIgnoreCase(1, "Vụ Hôm Nay")).thenReturn(false);
        when(cropCategoryRepository.findById(5)).thenReturn(Optional.of(category));
        when(userRepository.findById(10)).thenReturn(Optional.of(owner));
        when(cropSeasonRepository.save(any(CropSeason.class))).thenAnswer(i -> i.getArgument(0));

        CropSeasonRespDTO resp = cropSeasonService.createCropSeason(10, 1, req);

        assertEquals("active", resp.seasonStatus());
    }

    @Test
    @DisplayName("FE07_SEA_UTC03 [Boundary] - Tạo mùa vụ với ngày bắt đầu trong quá khứ -> Chặn bằng BadRequestException")
    void UTC03_createCropSeason_pastStartDate_throwsBadRequest() {
        CreateCropSeasonReqDTO req = new CreateCropSeasonReqDTO(
                "Vụ Quá Khứ", 5, null,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(60), null, null
        );
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));

        assertThrows(BadRequestException.class, () -> cropSeasonService.createCropSeason(10, 1, req));
        verify(cropSeasonRepository, never()).save(any());
    }

    @Test
    @DisplayName("FE07_SEA_UTC04 [Abnormal] - Tạo mùa vụ có endDate < startDate hoặc trùng tên mùa vụ -> Ném BadRequest / ResourceConflict")
    void UTC04_createCropSeason_endBeforeStartAndDuplicateName_rejected() {
        CreateCropSeasonReqDTO badDates = new CreateCropSeasonReqDTO(
                "Vụ Lỗi Ngày", 5, null,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(5), null, null
        );
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        assertThrows(BadRequestException.class, () -> cropSeasonService.createCropSeason(10, 1, badDates));

        CreateCropSeasonReqDTO dupName = new CreateCropSeasonReqDTO(
                "Vụ Đông Xuân 2027", 5, null,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(90), null, null
        );
        when(cropSeasonRepository.existsByFarmIdAndSeasonNameIgnoreCase(1, "Vụ Đông Xuân 2027")).thenReturn(true);
        assertThrows(ResourceConflictException.class, () -> cropSeasonService.createCropSeason(10, 1, dupName));
    }

    @Test
    @DisplayName("FE07_SEA_UTC05 [Normal] - Chỉnh sửa mùa vụ ở trạng thái planned hợp lệ -> Cập nhật thành công")
    void UTC05_updateCropSeason_plannedSeason_succeeds() {
        UpdateCropSeasonReqDTO req = new UpdateCropSeasonReqDTO(
                "Vụ Đông Xuân 2027 (Cập nhật)", 5, null,
                LocalDate.now().plusDays(20), LocalDate.now().plusDays(120), "Điều chỉnh lịch gieo"
        );
        when(cropSeasonRepository.findById(100)).thenReturn(Optional.of(plannedSeason));
        when(cropSeasonRepository.existsByFarmIdAndSeasonNameIgnoreCaseAndIdNot(1, "Vụ Đông Xuân 2027 (Cập nhật)", 100)).thenReturn(false);
        when(cropCategoryRepository.findById(5)).thenReturn(Optional.of(category));
        when(cropSeasonRepository.save(any(CropSeason.class))).thenAnswer(i -> i.getArgument(0));

        CropSeasonRespDTO resp = cropSeasonService.updateCropSeason(10, 100, req);

        assertEquals("Vụ Đông Xuân 2027 (Cập nhật)", resp.seasonName());
        assertEquals("Điều chỉnh lịch gieo", resp.notes());
    }

    @Test
    @DisplayName("FE07_SEA_UTC06 [Abnormal] - Chặn chỉnh sửa mùa vụ đang diễn ra (active) -> Ném BadRequestException")
    void UTC06_updateCropSeason_activeSeason_blockedWithBadRequest() {
        plannedSeason.setSeasonStatus("active");
        UpdateCropSeasonReqDTO req = new UpdateCropSeasonReqDTO(
                "Sửa Vụ Đang Chạy", 5, null, LocalDate.now().plusDays(5), LocalDate.now().plusDays(90), null
        );
        when(cropSeasonRepository.findById(100)).thenReturn(Optional.of(plannedSeason));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> cropSeasonService.updateCropSeason(10, 100, req));
        assertTrue(ex.getMessage().contains("đang diễn ra"));
        verify(cropSeasonRepository, never()).save(any());
    }

    @Test
    @DisplayName("FE07_SEA_UTC07 [Abnormal] - Chặn chỉnh sửa mùa vụ đã hoàn thành (completed) hoặc đã hủy (cancelled) -> Ném BadRequestException")
    void UTC07_updateCropSeason_completedOrCancelledSeason_blockedWithBadRequest() {
        UpdateCropSeasonReqDTO req = new UpdateCropSeasonReqDTO(
                "Sửa Vụ Cũ", 5, null, LocalDate.now().plusDays(5), LocalDate.now().plusDays(90), null
        );

        plannedSeason.setSeasonStatus("completed");
        when(cropSeasonRepository.findById(100)).thenReturn(Optional.of(plannedSeason));
        assertThrows(BadRequestException.class, () -> cropSeasonService.updateCropSeason(10, 100, req));

        plannedSeason.setSeasonStatus("cancelled");
        assertThrows(BadRequestException.class, () -> cropSeasonService.updateCropSeason(10, 100, req));
        verify(cropSeasonRepository, never()).save(any());
    }

    @Test
    @DisplayName("FE07_SEA_UTC08 [Normal] - Chuyển trạng thái sang completed khi chưa có endDate -> Tự động gán ngày kết thúc là hôm nay")
    void UTC08_updateStatus_completedWithoutEndDate_setsEndDateToday() {
        plannedSeason.setSeasonStatus("active");
        plannedSeason.setEndDate(null);
        when(cropSeasonRepository.findById(100)).thenReturn(Optional.of(plannedSeason));
        when(cropSeasonRepository.save(any(CropSeason.class))).thenAnswer(i -> i.getArgument(0));

        CropSeasonRespDTO resp = cropSeasonService.updateCropSeasonStatus(10, 100, new UpdateCropSeasonStatusReqDTO("completed"));

        assertEquals("completed", resp.seasonStatus());
        assertEquals(LocalDate.now(), plannedSeason.getEndDate());
    }

    @Test
    @DisplayName("FE07_SEA_UTC09 [Abnormal] - Chặn xóa mùa vụ đang diễn ra (active) hoặc đã có nhật ký canh tác/thu hoạch liên kết")
    void UTC09_deleteCropSeason_activeOrHasLinkedRecords_blockedWithBadRequest() {
        plannedSeason.setSeasonStatus("active");
        when(cropSeasonRepository.findById(100)).thenReturn(Optional.of(plannedSeason));
        assertThrows(BadRequestException.class, () -> cropSeasonService.deleteCropSeason(10, 100));

        plannedSeason.setSeasonStatus("planned");
        when(farmingActivityRepository.existsByCropSeasonId(100)).thenReturn(true);
        assertThrows(BadRequestException.class, () -> cropSeasonService.deleteCropSeason(10, 100));
        verify(cropSeasonRepository, never()).delete(any(CropSeason.class));
    }

    @Test
    @DisplayName("FE07_SEA_UTC10 [Normal] - Xóa mùa vụ planned chưa có nhật ký hay thu hoạch liên kết -> Xóa thành công")
    void UTC10_deleteCropSeason_cleanPlannedSeason_deletesSuccessfully() {
        when(cropSeasonRepository.findById(100)).thenReturn(Optional.of(plannedSeason));
        when(farmingActivityRepository.existsByCropSeasonId(100)).thenReturn(false);
        when(harvestRecordRepository.existsByCropSeasonId(100)).thenReturn(false);

        cropSeasonService.deleteCropSeason(10, 100);

        verify(cropSeasonRepository).delete(plannedSeason);
    }
}
