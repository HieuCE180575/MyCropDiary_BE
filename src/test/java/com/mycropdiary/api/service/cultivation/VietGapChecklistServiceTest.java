package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.vietgap.ChecklistResultItemRespDTO;
import com.mycropdiary.api.dto.vietgap.ChecklistResultRespDTO;
import com.mycropdiary.api.dto.vietgap.InitVietGapAuditRunReqDTO;
import com.mycropdiary.api.dto.vietgap.UpdateChecklistItemReqDTO;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.SoilAssessment;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.WaterAssessment;
import com.mycropdiary.api.entity.Zone;
import com.mycropdiary.api.entity.cultivation.*;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.BlockRepository;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.SoilAssessmentRepository;
import com.mycropdiary.api.repository.WaterAssessmentRepository;
import com.mycropdiary.api.repository.cultivation.*;
import com.mycropdiary.api.service.impl.ChecklistReviewServiceImpl;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VietGapChecklistServiceTest {

    @Mock
    private ChecklistRuleRepository checklistRuleRepository;
    @Mock
    private ChecklistReviewRepository checklistRunRepository;
    @Mock
    private ChecklistResultRepository checklistResultRepository;
    @Mock
    private CropSeasonRepository cropSeasonRepository;
    @Mock
    private BlockRepository blockRepository;
    @Mock
    private FarmMemberRepository farmMemberRepository;
    @Mock
    private FarmRepository farmRepository;
    @Mock
    private SoilAssessmentRepository soilAssessmentRepository;
    @Mock
    private WaterAssessmentRepository waterAssessmentRepository;
    @Mock
    private FarmingActivityRepository farmingActivityRepository;
    @Mock
    private HarvestRecordRepository harvestRecordRepository;
    @Mock
    private TrainingRecordRepository trainingRecordRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private MaterialUsageRepository materialUsageRepository;
    @Mock
    private VietGapPdfExportService pdfExportService;
    @Mock
    private FarmAccessGuard accessGuard;

    @InjectMocks
    private ChecklistReviewServiceImpl checklistService;

    private Farm farm;
    private Zone zone;
    private Block block;
    private CropSeason season;
    private FarmMember member;
    private User user;

    @BeforeEach
    void setUp() {
        farm = new Farm();
        farm.setId(1);
        farm.setFarmName("Trang Trại Xanh VietGAP");

        zone = new Zone();
        zone.setId(1);
        zone.setFarm(farm);
        zone.setZoneName("Khu Nhà Màng A");

        block = new Block();
        block.setId(1);
        block.setZone(zone);
        block.setBlockName("Lô A1 - Dưa Lưới");

        user = new User();
        user.setId(10);
        user.setFullName("Nguyễn Văn Kỹ Sư");

        member = new FarmMember();
        member.setId(100);
        member.setFarm(farm);
        member.setUser(user);
        member.setFarmRole("OWNER");
        member.setStatus("active");

        season = new CropSeason();
        season.setId(1);
        season.setFarm(farm);
        season.setSeasonName("Dưa lưới vụ Xuân 2026");
        season.setStartDate(LocalDate.of(2026, 1, 1));
        season.setEndDate(LocalDate.of(2026, 4, 30));
        season.setSeasonStatus("active");
    }

    @Nested
    @DisplayName("Tests khởi tạo đợt tự đánh giá VietGAP (initVietGapAuditRun)")
    class InitVietGapAuditRunTests {

        @Test
        @DisplayName("Khởi tạo thành công đợt đánh giá 33 tiêu chí VietGAP và tự động đối soát")
        void testInitVietGapAuditRun_Success() {
            InitVietGapAuditRunReqDTO req = new InitVietGapAuditRunReqDTO(
                    1, 1, null, Instant.now(), "Đợt tự đánh giá nội bộ trước thu hoạch"
            );

            when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
            when(cropSeasonRepository.findById(1)).thenReturn(Optional.of(season));
            when(blockRepository.findById(1)).thenReturn(Optional.of(block));
            when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1, 10, "active")).thenReturn(Optional.of(member));

            // Giả lập 2 rule Master: Đất nước và PHI thuốc
            ChecklistRule rule1 = new ChecklistRule(null, null, "VG-NL-01", "Kiểm tra chất lượng đất và nước tưới",
                    "Định kỳ 12 tháng", "NGUỒN_LỰC", "MUST", "ALL", true);
            rule1.setId(101);

            ChecklistRule rule2 = new ChecklistRule(null, null, "VG-TH-01", "Thời gian cách ly (PHI) trước thu hoạch",
                    "Tối thiểu 14 ngày", "THU_HOẠCH", "MUST", "ALL", true);
            rule2.setId(102);

            when(checklistRuleRepository.findSystemMasterRules()).thenReturn(List.of(rule1, rule2));

            // Giả lập dữ liệu đất nước đạt
            SoilAssessment soil = new SoilAssessment();
            soil.setId(1);
            soil.setBlock(block);
            soil.setSoilPh(new BigDecimal("6.50"));
            soil.setAssessedAt(Instant.now().minus(30, ChronoUnit.DAYS));
            when(soilAssessmentRepository.findByBlockIdOrderByAssessedAtDesc(1)).thenReturn(List.of(soil));

            WaterAssessment water = new WaterAssessment();
            water.setId(1);
            water.setAssessedAt(Instant.now().minus(30, ChronoUnit.DAYS));
            when(waterAssessmentRepository.findByFarmIdOrderByAssessedAtDesc(1)).thenReturn(List.of(water));

            // Giả lập không có thuốc BVTV độc hại (pass PHI)
            when(farmingActivityRepository.findAll(any(Specification.class))).thenReturn(List.of());

            when(checklistRunRepository.save(any(ChecklistRun.class))).thenAnswer(invocation -> {
                ChecklistRun r = invocation.getArgument(0);
                r.setId(50);
                return r;
            });

            when(checklistResultRepository.save(any(ChecklistResult.class))).thenAnswer(invocation -> {
                ChecklistResult res = invocation.getArgument(0);
                res.setId(200);
                return res;
            });

            ChecklistResultRespDTO resp = checklistService.initVietGapAuditRun(10, 1, req);

            assertNotNull(resp);
            assertEquals(50, resp.runId());
            assertEquals("in_progress", resp.runStatus());
            assertEquals(2, resp.totalItems());
            assertEquals(2, resp.passedItems());
            assertEquals(100.0, resp.compliancePercent());
            assertEquals(2, resp.results().size());

            verify(checklistRunRepository, times(2)).save(any(ChecklistRun.class));
            verify(checklistResultRepository, times(2)).save(any(ChecklistResult.class));
        }

        @Test
        @DisplayName("Ném ResourceNotFoundException khi mùa vụ không tồn tại")
        void testInitVietGapAuditRun_SeasonNotFound() {
            InitVietGapAuditRunReqDTO req = new InitVietGapAuditRunReqDTO(999, null, null, null, null);
            when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
            when(cropSeasonRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    checklistService.initVietGapAuditRun(10, 1, req));
        }

        @Test
        @DisplayName("Ném BadRequestException khi mùa vụ thuộc trang trại khác")
        void testInitVietGapAuditRun_SeasonDifferentFarm() {
            Farm otherFarm = new Farm();
            otherFarm.setId(99);
            season.setFarm(otherFarm);

            InitVietGapAuditRunReqDTO req = new InitVietGapAuditRunReqDTO(1, null, null, null, null);
            when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
            when(cropSeasonRepository.findById(1)).thenReturn(Optional.of(season));

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    checklistService.initVietGapAuditRun(10, 1, req));
            assertTrue(ex.getMessage().contains("Mùa vụ không thuộc trang trại này"));
        }

        @Test
        @DisplayName("Ném BadRequestException khi lô đất thuộc trang trại khác")
        void testInitVietGapAuditRun_BlockDifferentFarm() {
            Farm otherFarm = new Farm();
            otherFarm.setId(99);
            Zone otherZone = new Zone();
            otherZone.setFarm(otherFarm);
            block.setZone(otherZone);

            InitVietGapAuditRunReqDTO req = new InitVietGapAuditRunReqDTO(1, 1, null, null, null);
            when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
            when(cropSeasonRepository.findById(1)).thenReturn(Optional.of(season));
            when(blockRepository.findById(1)).thenReturn(Optional.of(block));

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    checklistService.initVietGapAuditRun(10, 1, req));
            assertTrue(ex.getMessage().contains("Lô đất không thuộc trang trại này"));
        }
    }

    @Nested
    @DisplayName("Tests thuật toán đối soát tự động (PHI, Đất Nước, Hoạt động, Đào tạo)")
    class AutoCheckAlgorithmTests {

        @Test
        @DisplayName("VG-TH-01: Đạt chuẩn PHI khi khoảng cách phun thuốc tới ngày thu hoạch >= 14 ngày")
        void testPhi_PassWhen14DaysOrMore() {
            ChecklistRun run = new ChecklistRun();
            run.setId(1);
            run.setFarm(farm);
            run.setCropSeason(season);
            run.setRunStatus("in_progress");

            ChecklistRule rule = new ChecklistRule(null, null, "VG-TH-01", "Cách ly thuốc BVTV",
                    "PHI", "THU_HOẠCH", "MUST", "ALL", true);
            ChecklistResult result = new ChecklistResult();
            result.setId(10);
            result.setChecklistRun(run);
            result.setChecklistRule(rule);
            result.setRuleCode("VG-TH-01");

            when(checklistRunRepository.findByIdWithDetails(1)).thenReturn(Optional.of(run));
            when(checklistResultRepository.findByRunIdWithDetails(1)).thenReturn(List.of(result));

            // Lần phun thuốc ngày 01/10/2026 (trước thu hoạch 19 ngày)
            FarmingActivity spray = new FarmingActivity();
            spray.setId(1);
            spray.setActivityType("Phun thuốc BVTV sinh học");
            spray.setPerformedAt(LocalDate.of(2026, 10, 1).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());

            when(farmingActivityRepository.findAll(any(Specification.class))).thenReturn(new ArrayList<>(List.of(spray)));

            // Ngày thu hoạch: 20/10/2026
            HarvestRecord harvest = new HarvestRecord();
            harvest.setId(1);
            harvest.setHarvestDate(LocalDate.of(2026, 10, 20));
            when(harvestRecordRepository.findByCropSeasonId(1)).thenReturn(new ArrayList<>(List.of(harvest)));

            checklistService.autoCheckRun(10, 1, 1);

            assertEquals("pass", result.getResultStatus());
            assertTrue(result.getIsAutoEvaluated());
            assertTrue(result.getNotes().contains("ĐẠT CHUẨN PHI"));
            assertTrue(result.getNotes().contains("19 ngày"));
            assertEquals("FARMING_ACTIVITY", result.getEvidenceReferenceType());
        }

        @Test
        @DisplayName("VG-TH-01: Cảnh báo vi phạm PHI khi khoảng cách phun thuốc tới ngày thu hoạch < 14 ngày")
        void testPhi_FailWhenLessThan14Days() {
            ChecklistRun run = new ChecklistRun();
            run.setId(1);
            run.setFarm(farm);
            run.setCropSeason(season);
            run.setRunStatus("in_progress");

            ChecklistRule rule = new ChecklistRule(null, null, "VG-TH-01", "Cách ly thuốc BVTV",
                    "PHI", "THU_HOẠCH", "MUST", "ALL", true);
            ChecklistResult result = new ChecklistResult();
            result.setId(10);
            result.setChecklistRun(run);
            result.setChecklistRule(rule);
            result.setRuleCode("VG-TH-01");

            when(checklistRunRepository.findByIdWithDetails(1)).thenReturn(Optional.of(run));
            when(checklistResultRepository.findByRunIdWithDetails(1)).thenReturn(List.of(result));

            // Lần phun thuốc ngày 15/10/2026, thu hoạch ngày 20/10/2026 -> cách 5 ngày (< 14)
            FarmingActivity spray = new FarmingActivity();
            spray.setId(1);
            spray.setActivityType("Phun thuốc trừ sâu");
            spray.setPerformedAt(LocalDate.of(2026, 10, 15).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());

            when(farmingActivityRepository.findAll(any(Specification.class))).thenReturn(new ArrayList<>(List.of(spray)));

            HarvestRecord harvest = new HarvestRecord();
            harvest.setId(1);
            harvest.setHarvestDate(LocalDate.of(2026, 10, 20));
            when(harvestRecordRepository.findByCropSeasonId(1)).thenReturn(new ArrayList<>(List.of(harvest)));

            checklistService.autoCheckRun(10, 1, 1);

            assertEquals("fail", result.getResultStatus());
            assertTrue(result.getIsAutoEvaluated());
            assertTrue(result.getNotes().contains("CẢNH BÁO VI PHẠM PHI"));
            assertTrue(result.getNotes().contains("5 ngày"));
        }

        @Test
        @DisplayName("VG-NL-01: Cảnh báo không đạt khi kết quả kiểm nghiệm Đất quá 12 tháng")
        void testSoilWater_FailWhenSoilOutdated() {
            ChecklistRun run = new ChecklistRun();
            run.setId(1);
            run.setFarm(farm);
            run.setCropSeason(season);
            run.setRunStatus("in_progress");

            ChecklistResult result = new ChecklistResult();
            result.setId(10);
            result.setChecklistRun(run);
            result.setRuleCode("VG-NL-01");

            when(checklistRunRepository.findByIdWithDetails(1)).thenReturn(Optional.of(run));
            when(checklistResultRepository.findByRunIdWithDetails(1)).thenReturn(List.of(result));

            // Đất đo cách đây 400 ngày (> 365 ngày)
            SoilAssessment outdatedSoil = new SoilAssessment();
            outdatedSoil.setId(1);
            outdatedSoil.setAssessedAt(Instant.now().minus(400, ChronoUnit.DAYS));
            when(soilAssessmentRepository.findByFarmIdOrderByAssessedAtDesc(1)).thenReturn(List.of(outdatedSoil));

            // Nước đo gần đây
            WaterAssessment water = new WaterAssessment();
            water.setId(1);
            water.setAssessedAt(Instant.now().minus(20, ChronoUnit.DAYS));
            when(waterAssessmentRepository.findByFarmIdOrderByAssessedAtDesc(1)).thenReturn(List.of(water));

            checklistService.autoCheckRun(10, 1, 1);

            assertEquals("fail", result.getResultStatus());
            assertTrue(result.getNotes().contains("Thiếu phiếu kiểm nghiệm mẫu Đất/giá thể trong vòng 12 tháng"));
        }

        @Test
        @DisplayName("VG-HS-05: Đạt khi có >= 5 hoạt động canh tác điện tử được ghi nhận")
        void testFarmingActivity_PassWhenAtLeast5Activities() {
            ChecklistRun run = new ChecklistRun();
            run.setId(1);
            run.setFarm(farm);
            run.setCropSeason(season);
            run.setRunStatus("in_progress");

            ChecklistResult result = new ChecklistResult();
            result.setId(10);
            result.setChecklistRun(run);
            result.setRuleCode("VG-HS-05");

            when(checklistRunRepository.findByIdWithDetails(1)).thenReturn(Optional.of(run));
            when(checklistResultRepository.findByRunIdWithDetails(1)).thenReturn(List.of(result));
            when(farmingActivityRepository.countByCropSeasonId(1)).thenReturn(12L);

            checklistService.autoCheckRun(10, 1, 1);

            assertEquals("pass", result.getResultStatus());
            assertTrue(result.getNotes().contains("Đã ghi nhận 12 hoạt động"));
        }

        @Test
        @DisplayName("VG-HS-01: Đạt khi trang trại có chứng chỉ/hồ sơ tập huấn VietGAP còn hiệu lực")
        void testTrainingRecord_PassWhenTrainingPresent() {
            ChecklistRun run = new ChecklistRun();
            run.setId(1);
            run.setFarm(farm);
            run.setCropSeason(season);
            run.setRunStatus("in_progress");

            ChecklistResult result = new ChecklistResult();
            result.setId(10);
            result.setChecklistRun(run);
            result.setRuleCode("VG-HS-01");

            when(checklistRunRepository.findByIdWithDetails(1)).thenReturn(Optional.of(run));
            when(checklistResultRepository.findByRunIdWithDetails(1)).thenReturn(List.of(result));

            TrainingRecord tr = new TrainingRecord();
            tr.setId(5);
            tr.setTrainingTitle("Tập huấn Quy trình VietGAP rau ăn quả năm 2026");
            tr.setTrainingDate(LocalDate.of(2026, 2, 10));
            when(trainingRecordRepository.findByFarmIdOrderByTrainingDateDesc(1)).thenReturn(List.of(tr));

            checklistService.autoCheckRun(10, 1, 1);

            assertEquals("pass", result.getResultStatus());
            assertTrue(result.getNotes().contains("Tập huấn Quy trình VietGAP rau ăn quả"));
            assertEquals("TRAINING_RECORD", result.getEvidenceReferenceType());
        }

        @Test
        @DisplayName("VG-CS-04: Đạt khi trang trại đã cập nhật sơ đồ mặt bằng tổng thể (farmMapUrl)")
        void testFarmMap_PassWhenFarmMapUrlPresent() {
            ChecklistRun run = new ChecklistRun();
            run.setId(1);
            run.setFarm(farm);
            run.setCropSeason(season);
            run.setRunStatus("in_progress");

            ChecklistResult result = new ChecklistResult();
            result.setId(11);
            result.setChecklistRun(run);
            result.setRuleCode("VG-CS-04");

            farm.setFarmMapUrl("https://storage.mycropdiary.com/maps/farm1-layout.png");
            when(checklistRunRepository.findByIdWithDetails(1)).thenReturn(Optional.of(run));
            when(checklistResultRepository.findByRunIdWithDetails(1)).thenReturn(List.of(result));
            when(farmRepository.findById(1)).thenReturn(Optional.of(farm));

            checklistService.autoCheckRun(10, 1, 1);

            assertEquals("pass", result.getResultStatus());
            assertTrue(result.getNotes().contains("Đã có sơ đồ mặt bằng tổng thể"));
            assertEquals("FARM", result.getEvidenceReferenceType());
        }

        @Test
        @DisplayName("VG-CS-01: Đạt khi trang trại có kho chứa vật tư riêng biệt (kho_thuoc_bvtv, kho_phan_bon)")
        void testWarehouse_PassWhenWarehousesPresent() {
            ChecklistRun run = new ChecklistRun();
            run.setId(1);
            run.setFarm(farm);
            run.setCropSeason(season);
            run.setRunStatus("in_progress");

            ChecklistResult result = new ChecklistResult();
            result.setId(12);
            result.setChecklistRun(run);
            result.setRuleCode("VG-CS-01");

            Warehouse w1 = new Warehouse(farm, "Kho Thuốc BVTV", "Khu cách ly A", null);
            w1.setId(101);
            w1.setWarehouseType("kho_thuoc_bvtv");

            Warehouse w2 = new Warehouse(farm, "Kho Phân Bón", "Khu nhà kính B", null);
            w2.setId(102);
            w2.setWarehouseType("kho_phan_bon");

            when(checklistRunRepository.findByIdWithDetails(1)).thenReturn(Optional.of(run));
            when(checklistResultRepository.findByRunIdWithDetails(1)).thenReturn(List.of(result));
            when(warehouseRepository.findByFarmIdOrderByWarehouseNameAsc(1)).thenReturn(List.of(w1, w2));

            checklistService.autoCheckRun(10, 1, 1);

            assertEquals("pass", result.getResultStatus());
            assertTrue(result.getNotes().contains("phân loại riêng biệt kho phân bón và thuốc BVTV"));
            assertEquals("WAREHOUSE", result.getEvidenceReferenceType());
        }

        @Test
        @DisplayName("VG-TX-03: Đạt và tính toán chuẩn cân bằng khối lượng (Mass Balance 2026)")
        void testMassBalance_PassWhenHarvestAndDiscardedRecorded() {
            ChecklistRun run = new ChecklistRun();
            run.setId(1);
            run.setFarm(farm);
            run.setCropSeason(season);
            run.setRunStatus("in_progress");

            ChecklistResult result = new ChecklistResult();
            result.setId(13);
            result.setChecklistRun(run);
            result.setRuleCode("VG-TX-03");

            HarvestRecord h1 = new HarvestRecord();
            h1.setId(201);
            h1.setQuantity(new BigDecimal("1250.50"));
            h1.setDiscardedQuantity(new BigDecimal("25.50"));
            h1.setUnit("kg");

            when(checklistRunRepository.findByIdWithDetails(1)).thenReturn(Optional.of(run));
            when(checklistResultRepository.findByRunIdWithDetails(1)).thenReturn(List.of(result));
            when(harvestRecordRepository.findByCropSeasonId(1)).thenReturn(List.of(h1));

            checklistService.autoCheckRun(10, 1, 1);

            assertEquals("pass", result.getResultStatus());
            assertTrue(result.getNotes().contains("Mass Balance"));
            assertTrue(result.getNotes().contains("1250.50 kg"));
            assertTrue(result.getNotes().contains("25.50 kg"));
            assertEquals("HARVEST_RECORD", result.getEvidenceReferenceType());
        }

        @Test
        @DisplayName("VG-HS-06: Đạt tự động kiểm tra lưu trữ hồ sơ >= 24 tháng theo VietGAP 2026")
        void testRecordRetention_Pass24Months() {
            ChecklistRun run = new ChecklistRun();
            run.setId(1);
            run.setFarm(farm);
            run.setCropSeason(season);
            run.setRunStatus("in_progress");

            ChecklistResult result = new ChecklistResult();
            result.setId(14);
            result.setChecklistRun(run);
            result.setRuleCode("VG-HS-06");

            when(checklistRunRepository.findByIdWithDetails(1)).thenReturn(Optional.of(run));
            when(checklistResultRepository.findByRunIdWithDetails(1)).thenReturn(List.of(result));

            checklistService.autoCheckRun(10, 1, 1);

            assertEquals("pass", result.getResultStatus());
            assertTrue(result.getNotes().contains("24 tháng"));
        }
    }

    @Nested
    @DisplayName("Tests chỉnh sửa tiêu chí & tính bất biến của Snapshot (updateChecklistItem)")
    class UpdateChecklistItemTests {

        @Test
        @DisplayName("Cập nhật thành công tiêu chí khi đợt kiểm tra đang mở (in_progress)")
        void testUpdateChecklistItem_Success() {
            ChecklistRun run = new ChecklistRun();
            run.setId(1);
            run.setFarm(farm);
            run.setRunStatus("in_progress");

            ChecklistResult result = new ChecklistResult();
            result.setId(20);
            result.setChecklistRun(run);
            result.setResultStatus("pending");

            when(checklistRunRepository.findByIdWithDetails(1)).thenReturn(Optional.of(run));
            when(checklistResultRepository.findById(20)).thenReturn(Optional.of(result));
            when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1, 10, "active")).thenReturn(Optional.of(member));
            when(checklistResultRepository.save(any(ChecklistResult.class))).thenAnswer(i -> i.getArgument(0));

            UpdateChecklistItemReqDTO req = new UpdateChecklistItemReqDTO(
                    "pass",
                    "Đã bổ sung tài liệu và kiểm tra đạt yêu cầu",
                    "https://storage.mycropdiary.com/evidence/doc-pass.png",
                    "DOCUMENT",
                    15
            );

            ChecklistResultItemRespDTO updated = checklistService.updateChecklistItem(10, 1, 1, 20, req);

            assertEquals("pass", updated.resultStatus());
            assertEquals("Đã bổ sung tài liệu và kiểm tra đạt yêu cầu", updated.notes());
            assertEquals("https://storage.mycropdiary.com/evidence/doc-pass.png", updated.evidenceImageUrl());
            assertFalse(updated.isAutoEvaluated());
        }

        @Test
        @DisplayName("Chặn chỉnh sửa (bảo vệ Snapshot bất biến) khi đợt đánh giá đã hoàn tất (completed)")
        void testUpdateChecklistItem_FailWhenCompleted() {
            ChecklistRun completedRun = new ChecklistRun();
            completedRun.setId(1);
            completedRun.setFarm(farm);
            completedRun.setRunStatus("completed"); // Đã đóng gói hồ sơ pháp lý

            when(checklistRunRepository.findByIdWithDetails(1)).thenReturn(Optional.of(completedRun));

            UpdateChecklistItemReqDTO req = new UpdateChecklistItemReqDTO("pass", "Sửa đổi lén", null, null, null);

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    checklistService.updateChecklistItem(10, 1, 1, 20, req));

            assertTrue(ex.getMessage().contains("Snapshot bất biến"));
        }

        @Test
        @DisplayName("Ném BadRequestException khi trạng thái kết quả không hợp lệ")
        void testUpdateChecklistItem_InvalidStatus() {
            ChecklistRun run = new ChecklistRun();
            run.setId(1);
            run.setFarm(farm);
            run.setRunStatus("in_progress");

            ChecklistResult result = new ChecklistResult();
            result.setId(20);
            result.setChecklistRun(run);

            when(checklistRunRepository.findByIdWithDetails(1)).thenReturn(Optional.of(run));
            when(checklistResultRepository.findById(20)).thenReturn(Optional.of(result));

            UpdateChecklistItemReqDTO req = new UpdateChecklistItemReqDTO("invalid_status", null, null, null, null);

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    checklistService.updateChecklistItem(10, 1, 1, 20, req));

            assertTrue(ex.getMessage().contains("Trạng thái kiểm tra không hợp lệ"));
        }
    }

    @Nested
    @DisplayName("Tests khóa biên bản & hoàn tất đợt đánh giá (completeChecklistRun)")
    class CompleteChecklistRunTests {

        @Test
        @DisplayName("Hoàn tất và tính điểm % tuân thủ thành công khi tất cả tiêu chí đã được đánh giá")
        void testCompleteChecklistRun_Success() {
            ChecklistRun run = new ChecklistRun();
            run.setId(1);
            run.setFarm(farm);
            run.setRunStatus("in_progress");

            ChecklistResult r1 = new ChecklistResult();
            r1.setId(1);
            r1.setResultStatus("pass");

            ChecklistResult r2 = new ChecklistResult();
            r2.setId(2);
            r2.setResultStatus("fail");

            ChecklistResult r3 = new ChecklistResult();
            r3.setId(3);
            r3.setResultStatus("not_applicable");

            when(checklistRunRepository.findByIdWithDetails(1)).thenReturn(Optional.of(run));
            when(checklistResultRepository.findByRunIdWithDetails(1)).thenReturn(List.of(r1, r2, r3));
            when(checklistRunRepository.save(any(ChecklistRun.class))).thenAnswer(i -> i.getArgument(0));

            ChecklistResultRespDTO completed = checklistService.completeChecklistRun(10, 1, 1);

            assertEquals("completed", completed.runStatus());
            assertEquals(3, completed.totalItems());
            assertEquals(1, completed.passedItems());
            assertEquals(1, completed.failedItems());
            assertEquals(1, completed.naItems());
            // (1 pass) / (3 total - 1 na) = 50.0%
            assertEquals(50.0, completed.compliancePercent());
            assertNotNull(completed.completedAt());
        }

        @Test
        @DisplayName("Ném BadRequestException khi vẫn còn tiêu chí ở trạng thái pending")
        void testCompleteChecklistRun_FailWhenPendingExists() {
            ChecklistRun run = new ChecklistRun();
            run.setId(1);
            run.setFarm(farm);
            run.setRunStatus("in_progress");

            ChecklistResult r1 = new ChecklistResult();
            r1.setId(1);
            r1.setResultStatus("pass");

            ChecklistResult r2 = new ChecklistResult();
            r2.setId(2);
            r2.setResultStatus("pending"); // Chưa kiểm

            when(checklistRunRepository.findByIdWithDetails(1)).thenReturn(Optional.of(run));
            when(checklistResultRepository.findByRunIdWithDetails(1)).thenReturn(List.of(r1, r2));

            BadRequestException ex = assertThrows(BadRequestException.class, () ->
                    checklistService.completeChecklistRun(10, 1, 1));

            assertTrue(ex.getMessage().contains("Còn 1 tiêu chí chưa hoàn thành đánh giá"));
        }
    }

    @Nested
    @DisplayName("Tests xuất báo cáo PDF chuẩn A4 (exportAuditReportPdf)")
    class ExportPdfTests {

        @Test
        @DisplayName("Xuất báo cáo PDF thành công trả về mảng byte hợp lệ")
        void testExportAuditReportPdf_Success() {
            ChecklistRun run = new ChecklistRun();
            run.setId(1);
            run.setFarm(farm);
            run.setRunStatus("completed");

            when(checklistRunRepository.findByIdWithDetails(1)).thenReturn(Optional.of(run));
            when(checklistResultRepository.findByRunIdWithDetails(1)).thenReturn(List.of());
            byte[] mockPdf = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D}; // %PDF-
            when(pdfExportService.generateAuditReportPdf(any(ChecklistResultRespDTO.class))).thenReturn(mockPdf);

            byte[] pdfBytes = checklistService.exportAuditReportPdf(10, 1, 1);

            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
            assertEquals(mockPdf, pdfBytes);
            verify(pdfExportService, times(1)).generateAuditReportPdf(any(ChecklistResultRespDTO.class));
        }
    }
}
