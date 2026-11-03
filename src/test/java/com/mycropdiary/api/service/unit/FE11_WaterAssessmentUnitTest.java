package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.water.CreateWaterAssessmentReq;
import com.mycropdiary.api.dto.water.UpdateWaterAssessmentReq;
import com.mycropdiary.api.dto.water.WaterAssessmentResp;
import com.mycropdiary.api.entity.*;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.WaterAssessmentRepository;
import com.mycropdiary.api.repository.WaterSourceRepository;
import com.mycropdiary.api.service.impl.WaterAssessmentServiceImpl;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE11 - Water Assessment Service Unit Tests")
class FE11_WaterAssessmentUnitTest {

    @Mock
    private WaterAssessmentRepository waterRepository;

    @Mock
    private WaterSourceRepository waterSourceRepository;

    @Mock
    private FarmMemberRepository farmMemberRepository;

    @Mock
    private FarmAccessGuard accessGuard;

    @InjectMocks
    private WaterAssessmentServiceImpl waterService;

    private Farm testFarm;
    private Zone testZone;
    private Block testBlock;
    private WaterSource testSource;
    private WaterAssessment testAssessment;
    private FarmMember testMember;

    @BeforeEach
    void setUp() {
        testFarm = new Farm();
        testFarm.setId(1);
        testFarm.setFarmName("Da Lat VietGAP Farm");

        testZone = new Zone(testFarm, "Zone A", BigDecimal.valueOf(1000), "m2");
        testZone.setId(10);

        testBlock = new Block(testZone, "Block A1", BigDecimal.valueOf(500), "m2");
        testBlock.setId(100);

        testSource = new WaterSource(testFarm, "Hồ chứa lắng lọc", "Nước mặt", "Hệ thống lọc cát", null);
        testSource.setId(1);

        testMember = new FarmMember(testFarm, null, "owner", "active");
        testMember.setId(2);

        testAssessment = new WaterAssessment(
                testSource, testBlock, testMember,
                BigDecimal.valueOf(6.8), BigDecimal.valueOf(22.0),
                "DAT_CHUAN", "Mẫu nước đạt chuẩn VietGAP", null, Instant.now()
        );
        testAssessment.setId(500);
    }

    @Test
    @DisplayName("Tạo mới phiếu đánh giá chất lượng nước thành công")
    void createWaterAssessment_success() {
        when(accessGuard.requireBlockAccess(1, 100)).thenReturn(testBlock);
        when(waterSourceRepository.findByFarmIdOrderBySourceNameAsc(1)).thenReturn(List.of(testSource));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1, 1, "active")).thenReturn(Optional.of(testMember));
        when(waterRepository.save(any(WaterAssessment.class))).thenReturn(testAssessment);

        CreateWaterAssessmentReq req = new CreateWaterAssessmentReq(
                null, "Hồ chứa lắng lọc", "Nước mặt",
                BigDecimal.valueOf(6.8), BigDecimal.valueOf(22.0),
                "DAT_CHUAN", "Mẫu nước đạt chuẩn", null, Instant.now()
        );

        WaterAssessmentResp resp = waterService.createWaterAssessment(1, 100, req);
        assertNotNull(resp);
        assertEquals(500, resp.assessmentId());
        assertEquals("DAT_CHUAN", resp.qualityStatus());
    }

    @Test
    @DisplayName("UC-20.1: Xem lịch sử đánh giá nước của Block")
    void getWaterAssessmentHistory_success() {
        when(accessGuard.requireBlockAccess(1, 100)).thenReturn(testBlock);
        when(waterRepository.findByBlockIdOrderByAssessedAtDesc(100)).thenReturn(List.of(testAssessment));

        List<WaterAssessmentResp> list = waterService.getWaterAssessmentHistory(1, 100);
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(BigDecimal.valueOf(6.8), list.get(0).waterPh());
    }

    @Test
    @DisplayName("UC-20.1: Xem lịch sử đánh giá nước toàn trang trại")
    void getFarmWaterAssessmentHistory_success() {
        when(accessGuard.requireFarmAccess(1, 1)).thenReturn(testMember);
        when(waterRepository.findByFarmIdOrderByAssessedAtDesc(1)).thenReturn(List.of(testAssessment));

        List<WaterAssessmentResp> list = waterService.getFarmWaterAssessmentHistory(1, 1);
        assertNotNull(list);
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("Cập nhật phiếu đánh giá nước thành công")
    void updateWaterAssessment_success() {
        when(waterRepository.findById(500)).thenReturn(Optional.of(testAssessment));
        when(accessGuard.requireBlockAccess(1, 100)).thenReturn(testBlock);
        when(waterRepository.save(any(WaterAssessment.class))).thenReturn(testAssessment);

        UpdateWaterAssessmentReq req = new UpdateWaterAssessmentReq(
                null, null, null,
                BigDecimal.valueOf(7.0), BigDecimal.valueOf(23.0),
                "DAT_CHUAN", "Cập nhật ghi chú", null, Instant.now()
        );

        WaterAssessmentResp resp = waterService.updateWaterAssessment(1, 500, req);
        assertNotNull(resp);
    }
}
