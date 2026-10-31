package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.training.CreateTrainingRecordReq;
import com.mycropdiary.api.dto.training.TrainingAttendeeReq;
import com.mycropdiary.api.dto.training.TrainingRecordResp;
import com.mycropdiary.api.dto.training.UpdateTrainingRecordReq;
import com.mycropdiary.api.dto.vietgap.CreateVietGapCriterionReq;
import com.mycropdiary.api.dto.vietgap.UpdateVietGapCriterionReq;
import com.mycropdiary.api.dto.vietgap.VietGapCriterionResp;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.cultivation.AssessmentCriterion;
import com.mycropdiary.api.entity.cultivation.FarmWorker;
import com.mycropdiary.api.entity.cultivation.TrainingAttendee;
import com.mycropdiary.api.entity.cultivation.TrainingRecord;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.cultivation.AssessmentCriterionRepository;
import com.mycropdiary.api.repository.cultivation.FarmWorkerRepository;
import com.mycropdiary.api.repository.cultivation.TrainingAttendeeRepository;
import com.mycropdiary.api.repository.cultivation.TrainingRecordRepository;
import com.mycropdiary.api.service.impl.TrainingRecordServiceImpl;
import com.mycropdiary.api.service.impl.VietGapCriterionServiceImpl;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Training Records & VietGAP Criteria Service Unit Tests")
public class FE09_TrainingRecordAndVietGapCriterionUnitTest {

    @Mock
    private TrainingRecordRepository trainingRecordRepository;

    @Mock
    private TrainingAttendeeRepository attendeeRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private FarmMemberRepository farmMemberRepository;

    @Mock
    private FarmWorkerRepository farmWorkerRepository;

    @Mock
    private AssessmentCriterionRepository criterionRepository;

    @Mock
    private FarmAccessGuard accessGuard;

    @InjectMocks
    private TrainingRecordServiceImpl trainingService;

    @InjectMocks
    private VietGapCriterionServiceImpl criterionService;

    private Farm testFarm;
    private FarmMember testMember;
    private FarmWorker testWorker;
    private User testUser;

    @BeforeEach
    void setUp() {
        testFarm = new Farm();
        ReflectionTestUtils.setField(testFarm, "id", 1);
        testFarm.setFarmName("Trang trại Da Lat Clean Produce");

        testUser = new User();
        ReflectionTestUtils.setField(testUser, "id", 10);
        testUser.setFullName("Nguyen Van Nong Dan");

        testMember = new FarmMember();
        ReflectionTestUtils.setField(testMember, "id", 5);
        testMember.setFarm(testFarm);
        testMember.setUser(testUser);

        testWorker = new FarmWorker();
        ReflectionTestUtils.setField(testWorker, "id", 20);
        testWorker.setFarmMember(testMember);
        testWorker.setWorkerCode("WRK-001");
        testWorker.setJobTitle("Kỹ thuật viên chăm sóc");
    }

    @Test
    @DisplayName("Create Training Record: Thành công lưu hồ sơ và danh sách công nhân tham gia")
    void createTrainingRecord_success() {
        when(farmRepository.findById(1)).thenReturn(Optional.of(testFarm));
        when(farmMemberRepository.findById(5)).thenReturn(Optional.of(testMember));
        when(farmWorkerRepository.findById(20)).thenReturn(Optional.of(testWorker));

        when(trainingRecordRepository.save(any(TrainingRecord.class))).thenAnswer(invocation -> {
            TrainingRecord tr = invocation.getArgument(0);
            if (tr.getId() == null) {
                ReflectionTestUtils.setField(tr, "id", 100);
            }
            return tr;
        });

        CreateTrainingRecordReq req = new CreateTrainingRecordReq(
                "Tập huấn an toàn sử dụng thuốc BVTV VietGAP",
                "Hướng dẫn pha thuốc đúng nồng độ và sử dụng trang phục bảo hộ PPE",
                LocalDate.of(2026, 3, 15),
                "https://example.com/photo.jpg",
                5,
                List.of(new TrainingAttendeeReq(20, "attended", "Đạt chuẩn thực hành"))
        );

        TrainingRecordResp resp = trainingService.createTrainingRecord(10, 1, req);

        assertNotNull(resp);
        assertEquals(100, resp.trainingRecordId());
        assertEquals("Tập huấn an toàn sử dụng thuốc BVTV VietGAP", resp.trainingTitle());
        assertEquals(1, resp.attendeeCount());
        assertNotNull(resp.attendees());
        assertEquals(1, resp.attendees().size());
        assertEquals("WRK-001", resp.attendees().get(0).workerCode());
        assertEquals("attended", resp.attendees().get(0).attendanceStatus());

        verify(accessGuard).requireFarmAccess(10, 1);
        verify(trainingRecordRepository, atLeastOnce()).save(any(TrainingRecord.class));
    }

    @Test
    @DisplayName("View Training Record Details: Lấy thông tin chi tiết và danh sách điểm danh")
    void getTrainingRecordDetails_success() {
        TrainingRecord tr = new TrainingRecord();
        ReflectionTestUtils.setField(tr, "id", 100);
        tr.setFarm(testFarm);
        tr.setOrganizedBy(testMember);
        tr.setTrainingTitle("Hướng dẫn thu hái dưa lưới");
        tr.setTrainingDate(LocalDate.of(2026, 4, 1));

        TrainingAttendee att = new TrainingAttendee(tr, testWorker, "attended", "Thao tác tốt");
        tr.setAttendees(new ArrayList<>(List.of(att)));

        when(trainingRecordRepository.findById(100)).thenReturn(Optional.of(tr));

        TrainingRecordResp resp = trainingService.getTrainingRecordDetails(10, 100);

        assertNotNull(resp);
        assertEquals(100, resp.trainingRecordId());
        assertEquals(1, resp.attendeeCount());
        assertEquals("WRK-001", resp.attendees().get(0).workerCode());

        verify(accessGuard).requireFarmAccess(10, 1);
    }

    @Test
    @DisplayName("Update Training Record: Cập nhật tiêu đề và danh sách công nhân")
    void updateTrainingRecord_success() {
        TrainingRecord tr = new TrainingRecord();
        ReflectionTestUtils.setField(tr, "id", 100);
        tr.setFarm(testFarm);
        tr.setOrganizedBy(testMember);
        tr.setTrainingTitle("Tiêu đề cũ");
        tr.setTrainingDate(LocalDate.of(2026, 4, 1));
        tr.setAttendees(new ArrayList<>());

        when(trainingRecordRepository.findById(100)).thenReturn(Optional.of(tr));
        when(farmWorkerRepository.findById(20)).thenReturn(Optional.of(testWorker));
        when(trainingRecordRepository.save(any(TrainingRecord.class))).thenReturn(tr);

        UpdateTrainingRecordReq req = new UpdateTrainingRecordReq(
                "Tiêu đề mới cập nhật",
                "Mô tả chi tiết hơn",
                LocalDate.of(2026, 4, 2),
                null,
                null,
                List.of(new TrainingAttendeeReq(20, "attended", "Đã hoàn thành"))
        );

        TrainingRecordResp resp = trainingService.updateTrainingRecord(10, 100, req);

        assertNotNull(resp);
        assertEquals("Tiêu đề mới cập nhật", tr.getTrainingTitle());
        assertEquals(1, tr.getAttendees().size());
        verify(trainingRecordRepository).save(tr);
    }

    @Test
    @DisplayName("Create VietGAP Criterion from TCVN: Thành công tạo mới tiêu chuẩn")
    void createCriterion_success() {
        when(farmRepository.findById(1)).thenReturn(Optional.of(testFarm));
        when(criterionRepository.existsByFarmIdAndCriterionName(1, "Kiểm soát nước tưới")).thenReturn(false);

        when(criterionRepository.save(any(AssessmentCriterion.class))).thenAnswer(invocation -> {
            AssessmentCriterion c = invocation.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 50);
            return c;
        });

        CreateVietGapCriterionReq req = new CreateVietGapCriterionReq(
                "Kiểm soát nước tưới",
                "Nước tưới phải đạt chuẩn QCVN 39:2011",
                new BigDecimal("100.00"),
                "Lấy mẫu xét nghiệm vi sinh định kỳ",
                "TCVN 11892-1:2017",
                true
        );

        VietGapCriterionResp resp = criterionService.createCriterion(10, 1, req);

        assertNotNull(resp);
        assertEquals(50, resp.criterionId());
        assertEquals("Kiểm soát nước tưới", resp.criterionName());
        assertEquals("TCVN 11892-1:2017", resp.standardReference());
        assertEquals("Lấy mẫu xét nghiệm vi sinh định kỳ", resp.evaluationMethod());

        verify(accessGuard).requireFarmOwner(10, 1);
    }

    @Test
    @DisplayName("Create VietGAP Criterion: Báo lỗi trùng tên tiêu chuẩn")
    void createCriterion_duplicateNameThrows() {
        when(farmRepository.findById(1)).thenReturn(Optional.of(testFarm));
        when(criterionRepository.existsByFarmIdAndCriterionName(1, "Kiểm soát nước tưới")).thenReturn(true);

        CreateVietGapCriterionReq req = new CreateVietGapCriterionReq(
                "Kiểm soát nước tưới",
                "Mô tả",
                new BigDecimal("100.00"),
                "Phương pháp"
        );

        assertThrows(BadRequestException.class, () -> criterionService.createCriterion(10, 1, req));
    }

    @Test
    @DisplayName("Import TCVN Standard Criteria: Khởi tạo danh mục 10 tiêu chuẩn VietGAP TCVN 11892-1:2017")
    void importTcvnStandardCriteria_success() {
        when(farmRepository.findById(1)).thenReturn(Optional.of(testFarm));
        when(criterionRepository.existsByFarmIdAndCriterionName(eq(1), anyString())).thenReturn(false);

        when(criterionRepository.save(any(AssessmentCriterion.class))).thenAnswer(invocation -> {
            AssessmentCriterion c = invocation.getArgument(0);
            ReflectionTestUtils.setField(c, "id", 1);
            return c;
        });

        List<VietGapCriterionResp> list = criterionService.importTcvnStandardCriteria(10, 1);

        assertNotNull(list);
        assertEquals(10, list.size());
        assertTrue(list.get(0).criterionName().contains("TCVN 11892-1:2017"));

        verify(accessGuard).requireFarmOwner(10, 1);
        verify(criterionRepository, times(10)).save(any(AssessmentCriterion.class));
    }

    @Test
    @DisplayName("Update VietGAP Criterion and Evaluation Method: Cập nhật phương pháp đánh giá")
    void updateCriterion_success() {
        AssessmentCriterion c = new AssessmentCriterion(
                testFarm,
                "Quản lý đất",
                "Mô tả ban đầu\n[Phương pháp đánh giá]: Phương pháp cũ",
                new BigDecimal("100.00"),
                true
        );
        ReflectionTestUtils.setField(c, "id", 70);

        when(criterionRepository.findById(70)).thenReturn(Optional.of(c));
        when(criterionRepository.save(any(AssessmentCriterion.class))).thenReturn(c);

        UpdateVietGapCriterionReq req = new UpdateVietGapCriterionReq(
                "Quản lý đất trồng",
                "Mô tả mới",
                new BigDecimal("90.00"),
                "Phương pháp mới cập nhật",
                true
        );

        VietGapCriterionResp resp = criterionService.updateCriterion(10, 70, req);

        assertNotNull(resp);
        assertEquals("Quản lý đất trồng", c.getCriterionName());
        assertEquals("Phương pháp mới cập nhật", resp.evaluationMethod());
        assertEquals("Mô tả mới", resp.description());

        verify(accessGuard).requireFarmOwner(10, 1);
    }
}

