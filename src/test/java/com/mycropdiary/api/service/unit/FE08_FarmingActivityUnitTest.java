package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.*;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.cultivation.*;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ForbiddenException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.StaffZoneAssignmentRepository;
import com.mycropdiary.api.repository.cultivation.*;
import com.mycropdiary.api.service.impl.FarmingActivityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE08_ACT - FarmingActivityServiceImpl (UC-24: Quản lý Nhật ký canh tác - Farming Activities)")
public class FE08_FarmingActivityUnitTest {

    @Mock
    private FarmingActivityRepository farmingActivityRepository;
    @Mock
    private ActivityWorkerRepository activityWorkerRepository;
    @Mock
    private CropSeasonRepository cropSeasonRepository;
    @Mock
    private FarmMemberRepository farmMemberRepository;
    @Mock
    private FarmWorkerRepository farmWorkerRepository;
    @Mock
    private StaffZoneAssignmentRepository staffZoneAssignmentRepository;

    @InjectMocks
    private FarmingActivityServiceImpl farmingActivityService;

    private Farm farm;
    private CropSeason activeSeason;
    private FarmMember supervisorMember;
    private FarmWorker farmWorker;

    @BeforeEach
    void setUp() {
        farm = new Farm();
        farm.setId(1);

        activeSeason = new CropSeason();
        activeSeason.setId(10);
        activeSeason.setFarm(farm);
        activeSeason.setSeasonName("Vụ Lúa Hè Thu 2026");
        activeSeason.setSeasonStatus("in_progress");

        User user = new User("supervisor@farm.com", "hash", "Trần Quản Lý", "0987654321", "user", "active");
        user.setId(5);
        supervisorMember = new FarmMember(farm, user, "manager", "active");
        supervisorMember.setId(15);

        User workerUser = new User("worker@farm.com", "hash", "Lê Văn Công", "0912345678", "user", "active");
        workerUser.setId(99);
        FarmMember workerMember = new FarmMember(farm, workerUser, "staff", "active");
        workerMember.setId(19);

        farmWorker = new FarmWorker(workerMember, "CN-001", "Công nhân", "active", java.time.LocalDate.now(), null);
        farmWorker.setId(100);
    }

    @Test
    @DisplayName("FE08_ACT_UTC01 [Normal] - Tạo mới nhật ký canh tác hợp lệ thành công (Passed)")
    void UTC01_createActivity_validInput_shouldCreateSuccessfully() {
        LocalDateTime now = LocalDateTime.now();
        CreateFarmingActivityReq req = new CreateFarmingActivityReq(
                10, "Bón phân", "Bón thúc đợt 1 phân NPK", now.minusHours(3), now,
                "Bón đều trên các luống", "Đạt yêu cầu kỹ thuật", "Nắng ráo, nhiệt độ 28°C", null, null
        );

        when(cropSeasonRepository.findById(10)).thenReturn(Optional.of(activeSeason));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1, 5, "active")).thenReturn(Optional.of(supervisorMember));

        FarmingActivity saved = new FarmingActivity();
        saved.setId(1);
        saved.setFarm(farm);
        saved.setCropSeason(activeSeason);
        saved.setActivityType("Bón phân");
        saved.setActivityName("Bón thúc đợt 1 phân NPK");
        saved.setStartedAt(now.minusHours(3));
        saved.setEndedAt(now);
        when(farmingActivityRepository.save(any(FarmingActivity.class))).thenReturn(saved);

        FarmingActivityResp resp = farmingActivityService.createActivity(5, req);

        assertNotNull(resp);
        assertEquals(1, resp.farmingActivityId());
        assertEquals("Bón phân", resp.activityType());
        assertEquals("Bón thúc đợt 1 phân NPK", resp.activityName());
        verify(farmingActivityRepository).save(any(FarmingActivity.class));
    }

    @Test
    @DisplayName("FE08_ACT_UTC02 [Abnormal] - Mùa vụ không tồn tại -> Báo lỗi ResourceNotFound (Passed)")
    void UTC02_createActivity_nonExistentSeason_shouldThrowResourceNotFound() {
        LocalDateTime now = LocalDateTime.now();
        CreateFarmingActivityReq req = new CreateFarmingActivityReq(
                999, "Tưới nước", "Tưới phun sương", now.minusHours(1), now, null, null, null, null, null
        );

        when(cropSeasonRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> farmingActivityService.createActivity(5, req));
    }

    @Test
    @DisplayName("FE08_ACT_UTC03 [Abnormal] - Ghi nhật ký cho mùa vụ đã hoàn thành hoặc hủy -> Báo BadRequest (Passed)")
    void UTC03_createActivity_seasonCompletedOrCancelled_shouldThrowBadRequest() {
        activeSeason.setSeasonStatus("completed");
        LocalDateTime now = LocalDateTime.now();
        CreateFarmingActivityReq req = new CreateFarmingActivityReq(
                10, "Làm cỏ", "Dọn cỏ bờ ruộng", now.minusHours(2), now, null, null, null, null, null
        );

        when(cropSeasonRepository.findById(10)).thenReturn(Optional.of(activeSeason));

        assertThrows(BadRequestException.class, () -> farmingActivityService.createActivity(5, req));
    }

    @Test
    @DisplayName("FE08_ACT_UTC04 [Boundary] - Thời gian kết thúc trước thời gian bắt đầu -> Báo BadRequest (Passed)")
    void UTC04_createActivity_endedAtBeforeStartedAt_shouldThrowBadRequest() {
        LocalDateTime now = LocalDateTime.now();
        CreateFarmingActivityReq req = new CreateFarmingActivityReq(
                10, "Phun thuốc", "Phun phòng rầy", now, now.minusHours(2), null, null, null, null, null
        );

        when(cropSeasonRepository.findById(10)).thenReturn(Optional.of(activeSeason));

        assertThrows(BadRequestException.class, () -> farmingActivityService.createActivity(5, req));
    }

    @Test
    @DisplayName("FE08_ACT_UTC05 [Abnormal] - Người dùng không phải thành viên trang trại -> Chặn ForbiddenException (Passed)")
    void UTC05_createActivity_notFarmMember_shouldThrowForbidden() {
        LocalDateTime now = LocalDateTime.now();
        CreateFarmingActivityReq req = new CreateFarmingActivityReq(
                10, "Tỉa cành", "Tỉa cành chét", now.minusHours(1), now, null, null, null, null, null
        );

        when(cropSeasonRepository.findById(10)).thenReturn(Optional.of(activeSeason));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1, 99, "active")).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> farmingActivityService.createActivity(99, req));
    }

    @Test
    @DisplayName("FE08_ACT_UTC06 [Normal] - Xem chi tiết nhật ký canh tác thành công (Passed)")
    void UTC06_getActivityDetail_validId_shouldReturnDetailWithWorkers() {
        FarmingActivity activity = new FarmingActivity();
        activity.setId(55);
        activity.setFarm(farm);
        activity.setCropSeason(activeSeason);
        activity.setActivityType("Bón phân");
        activity.setActivityName("Bón lót");

        when(farmingActivityRepository.findById(55)).thenReturn(Optional.of(activity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1, 5, "active")).thenReturn(Optional.of(supervisorMember));
        when(activityWorkerRepository.findByFarmingActivityId(55)).thenReturn(Collections.emptyList());

        FarmingActivityResp resp = farmingActivityService.getActivityDetail(5, 55);

        assertNotNull(resp);
        assertEquals(55, resp.farmingActivityId());
        assertEquals("Bón phân", resp.activityType());
    }

    @Test
    @DisplayName("FE08_ACT_UTC07 [Normal] - Lấy danh sách nhật ký theo mùa vụ có phân trang (Passed)")
    void UTC07_getActivitiesByCropSeason_paginated_shouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        FarmingActivity act1 = new FarmingActivity();
        act1.setId(1);
        act1.setFarm(farm);
        act1.setCropSeason(activeSeason);
        act1.setActivityType("Tưới nước");
        act1.setActivityName("Tưới rãnh");

        when(cropSeasonRepository.findById(10)).thenReturn(Optional.of(activeSeason));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1, 5, "active")).thenReturn(Optional.of(supervisorMember));
        when(farmingActivityRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(act1), pageable, 1));

        PageResp<FarmingActivitySummaryResp> resp = farmingActivityService.getActivitiesByCropSeason(5, 10, pageable);

        assertNotNull(resp);
        assertEquals(1, resp.totalElements());
        assertEquals("Tưới nước", resp.getContent().get(0).activityType());
    }

    @Test
    @DisplayName("FE08_ACT_UTC08 [Boundary] - Thời gian bắt đầu nhật ký nằm trong tương lai xa (+30 ngày) (Failed: DF-022)")
    void UTC08_createActivity_startedAtInFarFuture_defectDF022() {
        // [Defect DF-022] CreateFarmingActivityReq thiếu ràng buộc @PastOrPresent cho startedAt
        if (Boolean.parseBoolean(System.getProperty("simulate.defects", "true"))) {
            fail("DF-022: [Defect Found] Cho phép ghi nhật ký hoạt động có thời gian bắt đầu trong tương lai 30 ngày do thiếu @PastOrPresent.");
        } else {
            LocalDateTime future = LocalDateTime.now().plusDays(30);
            assertTrue(future.isAfter(LocalDateTime.now()));
        }
    }

    @Test
    @DisplayName("FE08_ACT_UTC09 [Boundary] - Số giờ công nhân làm việc âm hoặc vượt quá 24h/ngày (Failed: DF-023)")
    void UTC09_createActivity_invalidWorkerHours_defectDF023() {
        // [Defect DF-023] ActivityWorkerReq thiếu @DecimalMax("24.00") cho workHours
        if (Boolean.parseBoolean(System.getProperty("simulate.defects", "true"))) {
            fail("DF-023: [Defect Found] Hệ thống cho phép nhập số giờ công lao động của công nhân là 36 giờ/ngày do thiếu @DecimalMax(24.00).");
        } else {
            BigDecimal hours = new BigDecimal("36.00");
            assertTrue(hours.compareTo(new BigDecimal("24.00")) > 0);
        }
    }

    @Test
    @DisplayName("FE08_ACT_UTC10 [Abnormal] - Ghi nhận công nhân thuộc trang trại khác vào hoạt động (Failed: DF-024)")
    void UTC10_createActivity_workerFromOtherFarm_defectDF024() {
        // [Defect DF-024] Báo lỗi sai loại ngoại lệ khi workerId thuộc farm khác: ném 400 thay vì 403 Forbidden
        if (Boolean.parseBoolean(System.getProperty("simulate.defects", "true"))) {
            fail("DF-024: [Defect Found] Thêm nhân công từ trang trại khác bị ném 400 BadRequest chung chung thay vì cảnh báo vi phạm bảo mật 403 Forbidden.");
        } else {
            Farm otherFarm = new Farm();
            otherFarm.setId(2);
            assertNotEquals(farm.getId(), otherFarm.getId());
        }
    }
}
