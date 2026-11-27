package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.cultivation.ActivityWorkerReqDTO;
import com.mycropdiary.api.dto.cultivation.CreateFarmActivityReqDTO;
import com.mycropdiary.api.dto.cultivation.FarmingActivityRespDTO;
import com.mycropdiary.api.dto.worker.CreateWorkerReqDTO;
import com.mycropdiary.api.dto.worker.FarmWorkerRespDTO;
import com.mycropdiary.api.dto.worker.UpdateWorkerReqDTO;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import com.mycropdiary.api.entity.cultivation.FarmWorker;
import com.mycropdiary.api.entity.cultivation.FarmingActivity;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ForbiddenException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.StaffZoneAssignmentRepository;
import com.mycropdiary.api.repository.cultivation.ActivityWorkerRepository;
import com.mycropdiary.api.repository.cultivation.CropSeasonRepository;
import com.mycropdiary.api.repository.cultivation.FarmWorkerRepository;
import com.mycropdiary.api.repository.cultivation.FarmingActivityRepository;
import com.mycropdiary.api.service.impl.FarmWorkerServiceImpl;
import com.mycropdiary.api.service.impl.FarmingActivityServiceImpl;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE05_EXT - External Farm Workers Without Accounts & Multi-Worker Activity Assignment Unit Tests (645e148)")
public class FE05_ExternalWorkerUnitTest {

    @Mock private FarmWorkerRepository farmWorkerRepository;
    @Mock private FarmMemberRepository farmMemberRepository;
    @Mock private FarmRepository farmRepository;
    @Mock private FarmAccessGuard accessGuard;
    @Mock private FarmingActivityRepository farmingActivityRepository;
    @Mock private ActivityWorkerRepository activityWorkerRepository;
    @Mock private CropSeasonRepository cropSeasonRepository;
    @Mock private StaffZoneAssignmentRepository staffZoneAssignmentRepository;

    private FarmWorkerServiceImpl farmWorkerService;
    private FarmingActivityServiceImpl farmingActivityService;

    private Farm farm;
    private Farm otherFarm;
    private User ownerUser;
    private User memberUser;
    private FarmMember ownerMember;
    private FarmMember staffMember;
    private CropSeason activeSeason;

    @BeforeEach
    void setUp() {
        farmWorkerService = new FarmWorkerServiceImpl(farmWorkerRepository, farmMemberRepository, accessGuard);
        ReflectionTestUtils.setField(farmWorkerService, "farmRepository", farmRepository);

        farmingActivityService = new FarmingActivityServiceImpl(
                farmingActivityRepository, activityWorkerRepository, cropSeasonRepository,
                farmMemberRepository, farmWorkerRepository, staffZoneAssignmentRepository
        );

        farm = new Farm();
        farm.setId(10);
        farm.setFarmName("Nông trại Cần Thơ");

        otherFarm = new Farm();
        otherFarm.setId(99);
        otherFarm.setFarmName("Nông trại Khác");

        ownerUser = new User("owner@farm.vn", "hash", "Chủ Trại", "0901111111", "owner", "active");
        ownerUser.setId(1);

        memberUser = new User("staff@farm.vn", "hash", "Nguyễn Văn B", "0912345678", "staff", "active");
        memberUser.setId(2);

        ownerMember = new FarmMember(farm, ownerUser, "owner", "active");
        ownerMember.setId(11);

        staffMember = new FarmMember(farm, memberUser, "member", "active");
        staffMember.setId(20);

        activeSeason = new CropSeason();
        activeSeason.setId(30);
        activeSeason.setFarm(farm);
        activeSeason.setSeasonStatus("active");
    }

    @Test
    @DisplayName("FE05_EXT_UTC01 [Normal] - Tạo hồ sơ công nhân bên ngoài (farmMemberId=null) không cần tài khoản hệ thống")
    void UTC01_createExternalWorker_succeedsWithoutAccount() {
        CreateWorkerReqDTO req = new CreateWorkerReqDTO(
                null, "  Lê Văn Thời Vụ  ", " 0987654321 ", " CN-TV-01 ",
                "Công nhân thu hoạch", null, LocalDate.of(2026, 11, 1), "Làm thời vụ"
        );
        when(farmRepository.findById(10)).thenReturn(Optional.of(farm));
        when(farmWorkerRepository.existsByFarmMemberFarmIdAndWorkerCodeIgnoreCase(10, "CN-TV-01")).thenReturn(false);
        when(farmWorkerRepository.save(any(FarmWorker.class))).thenAnswer(i -> {
            FarmWorker w = i.getArgument(0);
            w.setId(101);
            return w;
        });

        FarmWorkerRespDTO resp = farmWorkerService.createWorker(1, 10, req);

        assertNotNull(resp);
        assertNull(resp.farmMemberId());
        assertEquals("Lê Văn Thời Vụ", resp.fullName());
        assertEquals("0987654321", resp.phoneNumber());
        assertEquals("CN-TV-01", resp.workerCode());
        assertEquals("active", resp.employmentStatus());
        verify(farmMemberRepository, never()).findById(any());
    }

    @Test
    @DisplayName("FE05_EXT_UTC02 [Abnormal] - Tạo công nhân bên ngoài (farmMemberId=null) thiếu họ tên -> Ném BadRequestException")
    void UTC02_createExternalWorker_blankFullName_throwsBadRequest() {
        CreateWorkerReqDTO req = new CreateWorkerReqDTO(
                null, "   ", "0987654321", "CN-TV-02", "Thu hoạch", "seasonal", null, null
        );

        assertThrows(BadRequestException.class, () -> farmWorkerService.createWorker(1, 10, req));
        verify(farmWorkerRepository, never()).save(any());
    }

    @Test
    @DisplayName("FE05_EXT_UTC03 [Abnormal] - Tạo công nhân bên ngoài trùng mã công nhân trong cùng trang trại -> Ném BadRequestException")
    void UTC03_createExternalWorker_duplicateWorkerCode_throwsBadRequest() {
        CreateWorkerReqDTO req = new CreateWorkerReqDTO(
                null, "Trần Văn Trùng", "0909000111", "CN-TV-01", "Tưới tiêu", "active", null, null
        );
        when(farmRepository.findById(10)).thenReturn(Optional.of(farm));
        when(farmWorkerRepository.existsByFarmMemberFarmIdAndWorkerCodeIgnoreCase(10, "CN-TV-01")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> farmWorkerService.createWorker(1, 10, req));
        verify(farmWorkerRepository, never()).save(any());
    }

    @Test
    @DisplayName("FE05_EXT_UTC04 [Abnormal] - Tạo công nhân bên ngoài cho trang trại không tồn tại -> Ném ResourceNotFoundException")
    void UTC04_createExternalWorker_farmNotFound_throwsResourceNotFound() {
        CreateWorkerReqDTO req = new CreateWorkerReqDTO(
                null, "Phạm Thị A", "0909111222", "CN-TV-04", "Chăm sóc", "active", null, null
        );
        when(farmRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> farmWorkerService.createWorker(1, 999, req));
    }

    @Test
    @DisplayName("FE05_EXT_UTC05 [Normal] - Tạo công nhân liên kết FarmMember bỏ trống tên/SĐT -> Tự động lấy từ hồ sơ User")
    void UTC05_createMemberLinkedWorker_fallsBackToUserNameAndPhone() {
        CreateWorkerReqDTO req = new CreateWorkerReqDTO(
                20, null, null, "CN-NV-01", "Kỹ thuật viên", "active", LocalDate.of(2026, 10, 1), null
        );
        when(farmMemberRepository.findById(20)).thenReturn(Optional.of(staffMember));
        when(farmWorkerRepository.findByFarmMemberId(20)).thenReturn(Optional.empty());
        when(farmWorkerRepository.existsByFarmMemberFarmIdAndWorkerCodeIgnoreCase(10, "CN-NV-01")).thenReturn(false);
        when(farmWorkerRepository.save(any(FarmWorker.class))).thenAnswer(i -> {
            FarmWorker w = i.getArgument(0);
            w.setId(102);
            return w;
        });

        FarmWorkerRespDTO resp = farmWorkerService.createWorker(1, 10, req);

        assertEquals(20, resp.farmMemberId());
        assertEquals("Nguyễn Văn B", resp.fullName());
        assertEquals("0912345678", resp.phoneNumber());
    }

    @Test
    @DisplayName("FE05_EXT_UTC06 [Normal] - Cập nhật công nhân bên ngoài đổi họ tên và xóa trắng SĐT -> Lưu phoneNumber=null")
    void UTC06_updateExternalWorker_updatesNameAndClearsBlankPhone() {
        FarmWorker existing = new FarmWorker(farm, null, "Tên Cũ", "0901234567", "CN-TV-01", "Tưới", "active", null, null);
        existing.setId(101);
        UpdateWorkerReqDTO req = new UpdateWorkerReqDTO(
                null, "  Tên Mới Sau Sửa  ", "   ", "CN-TV-01", "Thu hoạch chính", "seasonal", null, "Cập nhật"
        );
        when(farmWorkerRepository.findByIdAndFarmMemberFarmId(101, 10)).thenReturn(Optional.of(existing));
        when(farmWorkerRepository.save(any(FarmWorker.class))).thenAnswer(i -> i.getArgument(0));

        FarmWorkerRespDTO resp = farmWorkerService.updateWorker(1, 10, 101, req);

        assertEquals("Tên Mới Sau Sửa", resp.fullName());
        assertNull(resp.phoneNumber());
        assertEquals("seasonal", resp.employmentStatus());
    }

    @Test
    @DisplayName("FE05_EXT_UTC07 [Abnormal] - Cập nhật công nhân đổi sang mã đã tồn tại trong trang trại -> Ném BadRequestException")
    void UTC07_updateExternalWorker_duplicateCode_throwsBadRequest() {
        FarmWorker existing = new FarmWorker(farm, null, "Lê Văn A", "0901234567", "CN-01", "Tưới", "active", null, null);
        existing.setId(101);
        UpdateWorkerReqDTO req = new UpdateWorkerReqDTO(
                null, "Lê Văn A", "0901234567", "CN-02", "Tưới", "active", null, null
        );
        when(farmWorkerRepository.findByIdAndFarmMemberFarmId(101, 10)).thenReturn(Optional.of(existing));
        when(farmWorkerRepository.existsByFarmMemberFarmIdAndWorkerCodeIgnoreCase(10, "CN-02")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> farmWorkerService.updateWorker(1, 10, 101, req));
    }

    @Test
    @DisplayName("FE05_EXT_UTC08 [Normal] - Lọc danh sách công nhân kết hợp cả công nhân ngoài và thành viên theo trạng thái & từ khóa")
    void UTC08_getWorkersByFarm_filtersExternalAndMemberWorkersByStatusAndKeyword() {
        FarmWorker extWorker = new FarmWorker(farm, null, "Lê Văn Thời Vụ", "0988111222", "CN-TV-01", "Thu hoạch", "seasonal", null, null);
        extWorker.setId(101);
        FarmWorker memberWorker = new FarmWorker(farm, staffMember, null, null, "CN-NV-01", "Phun thuốc", "active", null, null);
        memberWorker.setId(102);

        when(farmWorkerRepository.findByFarmId(10)).thenReturn(List.of(extWorker, memberWorker));

        List<FarmWorkerRespDTO> filtered = farmWorkerService.getWorkersByFarm(1, 10, "seasonal", "thời vụ");

        assertEquals(1, filtered.size());
        assertEquals("CN-TV-01", filtered.get(0).workerCode());
    }

    @Test
    @DisplayName("FE05_EXT_UTC09 [Normal] - Ghi nhật ký canh tác phân công nhiều công nhân bên ngoài (farmMember=null) cùng lúc")
    void UTC09_createActivity_assignsMultipleExternalWorkersInSameFarm() {
        FarmWorker ext1 = new FarmWorker(farm, null, "Công nhân Ngoài 1", "0901000001", "CN-EXT-1", "Làm cỏ", "seasonal", null, null);
        ext1.setId(201);
        FarmWorker ext2 = new FarmWorker(farm, null, "Công nhân Ngoài 2", "0901000002", "CN-EXT-2", "Bón phân", "seasonal", null, null);
        ext2.setId(202);

        CreateFarmActivityReqDTO req = new CreateFarmActivityReqDTO(
                30, "Chăm sóc", "Làm cỏ và bón lót",
                LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1),
                "Hoàn thành lô A", "Đạt", "Nắng nhẹ", null,
                List.of(
                        new ActivityWorkerReqDTO(201, new BigDecimal("4.0"), "Làm cỏ"),
                        new ActivityWorkerReqDTO(202, new BigDecimal("3.5"), "Bón phân")
                )
        );
        when(cropSeasonRepository.findById(30)).thenReturn(Optional.of(activeSeason));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(10, 1, "active")).thenReturn(Optional.of(ownerMember));
        when(farmingActivityRepository.save(any(FarmingActivity.class))).thenAnswer(i -> {
            FarmingActivity a = i.getArgument(0);
            a.setId(500);
            return a;
        });
        when(farmWorkerRepository.findByIdInAndFarmId(List.of(201, 202), 10)).thenReturn(List.of(ext1, ext2));

        FarmingActivityRespDTO resp = farmingActivityService.createActivity(1, req);

        assertNotNull(resp);
        assertEquals(2, resp.workers().size());
        assertEquals("Công nhân Ngoài 1", resp.workers().get(0).fullName());
        verify(activityWorkerRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("FE05_EXT_UTC10 [Abnormal] - Phân công công nhân bên ngoài thuộc trang trại khác -> Chặn bằng ForbiddenException")
    void UTC10_createActivity_externalWorkerFromOtherFarm_throwsForbidden() {
        FarmWorker foreignExtWorker = new FarmWorker(otherFarm, null, "Công nhân Trại Khác", "0909999999", "CN-EXT-99", "Thu hoạch", "active", null, null);
        foreignExtWorker.setId(999);

        CreateFarmActivityReqDTO req = new CreateFarmActivityReqDTO(
                30, "Thu hoạch", "Thu hoạch chéo trại",
                LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1),
                null, null, null, null,
                List.of(new ActivityWorkerReqDTO(999, new BigDecimal("2.0"), null))
        );
        when(cropSeasonRepository.findById(30)).thenReturn(Optional.of(activeSeason));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(10, 1, "active")).thenReturn(Optional.of(ownerMember));
        when(farmingActivityRepository.save(any(FarmingActivity.class))).thenAnswer(i -> {
            FarmingActivity a = i.getArgument(0);
            a.setId(501);
            return a;
        });
        when(farmWorkerRepository.findByIdInAndFarmId(List.of(999), 10)).thenReturn(List.of());
        when(farmWorkerRepository.findById(999)).thenReturn(Optional.of(foreignExtWorker));

        assertThrows(ForbiddenException.class, () -> farmingActivityService.createActivity(1, req));
        verify(activityWorkerRepository, never()).saveAll(anyList());
    }
}
