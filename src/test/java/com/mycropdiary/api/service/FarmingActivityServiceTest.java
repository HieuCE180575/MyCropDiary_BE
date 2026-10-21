package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.*;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.cultivation.*;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.cultivation.*;
import com.mycropdiary.api.service.impl.FarmingActivityServiceImpl;
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
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FarmingActivityServiceTest {

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

    @InjectMocks
    private FarmingActivityServiceImpl farmingActivityService;

    private Farm farm;
    private CropSeason activeSeason;
    private CropSeason completedSeason;
    private FarmMember supervisor;
    private User supervisorUser;
    private FarmingActivity sampleActivity;
    private FarmWorker farmWorker;

    @BeforeEach
    void setUp() {
        farm = new Farm();
        farm.setId(1);
        farm.setFarmName("Trang trại mẫu");

        supervisorUser = new User();
        supervisorUser.setId(10);
        supervisorUser.setFullName("Nguyễn Văn Giám Sát");

        supervisor = new FarmMember();
        supervisor.setId(100);
        supervisor.setFarm(farm);
        supervisor.setUser(supervisorUser);
        supervisor.setMembershipStatus("active");
        supervisor.setMemberRole("manager");

        activeSeason = new CropSeason();
        activeSeason.setId(1000);
        activeSeason.setFarm(farm);
        activeSeason.setSeasonName("Vụ Đông Xuân 2026");
        activeSeason.setSeasonStatus("active");

        completedSeason = new CropSeason();
        completedSeason.setId(1001);
        completedSeason.setFarm(farm);
        completedSeason.setSeasonName("Vụ Thu Đông 2025");
        completedSeason.setSeasonStatus("completed");

        sampleActivity = new FarmingActivity();
        sampleActivity.setId(500);
        sampleActivity.setFarm(farm);
        sampleActivity.setCropSeason(activeSeason);
        sampleActivity.setActivityName("Bón phân đợt 1");
        sampleActivity.setActivityType("Bón phân");
        sampleActivity.setStartedAt(LocalDateTime.now().minusHours(2));
        sampleActivity.setEndedAt(LocalDateTime.now().minusHours(1));
        sampleActivity.setDescription("Bón phân NPK cho luống rau");
        sampleActivity.setCreatedBy(supervisorUser);

        FarmMember workerMember = new FarmMember();
        workerMember.setFarm(farm);
        User workerUser = new User();
        workerUser.setFullName("Lê Văn Công Nhân");
        workerMember.setUser(workerUser);

        farmWorker = new FarmWorker();
        farmWorker.setId(200);
        farmWorker.setFarmMember(workerMember);
        farmWorker.setWorkerCode("CN01");
    }

    @Test
    @DisplayName("Tạo nhật ký canh tác thành công")
    void createActivity_Success() {
        CreateFarmingActivityReq req = new CreateFarmingActivityReq(
                activeSeason.getId(),
                "Tưới tiêu",
                "Tưới nước nhỏ giọt",
                LocalDateTime.now().minusHours(3),
                LocalDateTime.now().minusHours(2),
                "Tưới đợt sáng",
                "Đạt yêu cầu",
                "Nắng nhẹ",
                null,
                null
        );

        when(cropSeasonRepository.findById(activeSeason.getId())).thenReturn(Optional.of(activeSeason));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(farm.getId(), supervisorUser.getId(), "active"))
                .thenReturn(Optional.of(supervisor));
        when(farmingActivityRepository.save(any(FarmingActivity.class))).thenAnswer(invocation -> {
            FarmingActivity a = invocation.getArgument(0);
            a.setId(501);
            return a;
        });

        FarmingActivityResp resp = farmingActivityService.createActivity(supervisorUser.getId(), req);

        assertNotNull(resp);
        assertEquals(501, resp.farmingActivityId());
        assertEquals("Tưới tiêu", resp.activityType());
        assertEquals("Tưới nước nhỏ giọt", resp.activityName());
        verify(farmingActivityRepository).save(any(FarmingActivity.class));
    }

    @Test
    @DisplayName("Tạo nhật ký thất bại khi mùa vụ đã hoàn thành")
    void createActivity_SeasonCompleted_ThrowsBadRequest() {
        CreateFarmingActivityReq req = new CreateFarmingActivityReq(
                completedSeason.getId(),
                "Thu hoạch",
                "Thu hoạch sớm",
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),
                null, null, null, null, null
        );

        when(cropSeasonRepository.findById(completedSeason.getId())).thenReturn(Optional.of(completedSeason));

        assertThrows(BadRequestException.class, () ->
                farmingActivityService.createActivity(supervisorUser.getId(), req));
    }

    @Test
    @DisplayName("Cập nhật nhật ký canh tác thành công kèm danh sách công nhân")
    void updateActivity_Success() {
        UpdateFarmingActivityReq req = new UpdateFarmingActivityReq(
                "Phun thuốc",
                "Phun thuốc trừ sâu sinh học",
                LocalDateTime.now().minusHours(4),
                LocalDateTime.now().minusHours(2),
                "Phun toàn bộ luống",
                "Hoàn thành tốt",
                "Gió nhẹ",
                null,
                null,
                null,
                List.of(new ActivityWorkerReq(farmWorker.getId(), BigDecimal.valueOf(2.0), "Phun bình số 1"))
        );

        when(farmingActivityRepository.findById(sampleActivity.getId())).thenReturn(Optional.of(sampleActivity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(farm.getId(), supervisorUser.getId(), "active"))
                .thenReturn(Optional.of(supervisor));
        when(farmWorkerRepository.findByIdInAndFarmId(List.of(farmWorker.getId()), farm.getId()))
                .thenReturn(List.of(farmWorker));
        when(farmingActivityRepository.save(any(FarmingActivity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FarmingActivityResp resp = farmingActivityService.updateActivity(supervisorUser.getId(), sampleActivity.getId(), req);

        assertNotNull(resp);
        assertEquals("Phun thuốc", resp.activityType());
        assertEquals("Phun thuốc trừ sâu sinh học", resp.activityName());
        assertEquals(1, resp.workers().size());
        assertEquals("CN01", resp.workers().get(0).workerCode());

        verify(activityWorkerRepository).deleteByFarmingActivityId(sampleActivity.getId());
        verify(activityWorkerRepository).saveAll(anyList());
        verify(farmingActivityRepository).save(any(FarmingActivity.class));
    }

    @Test
    @DisplayName("Cập nhật nhật ký thất bại khi không tìm thấy nhật ký")
    void updateActivity_NotFound_ThrowsException() {
        UpdateFarmingActivityReq req = new UpdateFarmingActivityReq(
                "Phun thuốc", "Tên", LocalDateTime.now().minusHours(2), LocalDateTime.now(),
                null, null, null, null, null
        );

        when(farmingActivityRepository.findById(9999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                farmingActivityService.updateActivity(supervisorUser.getId(), 9999, req));
    }

    @Test
    @DisplayName("Cập nhật nhật ký thất bại khi thời gian kết thúc trước thời gian bắt đầu")
    void updateActivity_InvalidTime_ThrowsBadRequest() {
        UpdateFarmingActivityReq req = new UpdateFarmingActivityReq(
                "Phun thuốc", "Tên",
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().minusHours(3),
                null, null, null, null, null
        );

        when(farmingActivityRepository.findById(sampleActivity.getId())).thenReturn(Optional.of(sampleActivity));

        assertThrows(BadRequestException.class, () ->
                farmingActivityService.updateActivity(supervisorUser.getId(), sampleActivity.getId(), req));
    }

    @Test
    @DisplayName("Tìm kiếm và lọc hoạt động canh tác thành công")
    void searchAndFilterActivities_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        when(cropSeasonRepository.findById(activeSeason.getId())).thenReturn(Optional.of(activeSeason));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(farm.getId(), supervisorUser.getId(), "active"))
                .thenReturn(Optional.of(supervisor));
        when(farmingActivityRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(sampleActivity)));

        PageResp<FarmingActivitySummaryResp> result = farmingActivityService.searchAndFilterActivities(
                supervisorUser.getId(),
                activeSeason.getId(),
                "bón phân",
                "Bón phân",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now(),
                pageable
        );

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals("Bón phân đợt 1", result.content().get(0).activityName());
    }

    @Test
    @DisplayName("Xem chi tiết nhật ký canh tác thành công")
    void getActivityDetail_Success() {
        when(farmingActivityRepository.findById(sampleActivity.getId())).thenReturn(Optional.of(sampleActivity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(farm.getId(), supervisorUser.getId(), "active"))
                .thenReturn(Optional.of(supervisor));
        when(activityWorkerRepository.findByFarmingActivityId(sampleActivity.getId()))
                .thenReturn(List.of(new ActivityWorker(sampleActivity, farmWorker, BigDecimal.valueOf(1.5), "Hoàn tất")));

        FarmingActivityResp resp = farmingActivityService.getActivityDetail(supervisorUser.getId(), sampleActivity.getId());

        assertNotNull(resp);
        assertEquals(sampleActivity.getId(), resp.farmingActivityId());
        assertEquals(1, resp.workers().size());
        assertEquals("CN01", resp.workers().get(0).workerCode());
    }
}
