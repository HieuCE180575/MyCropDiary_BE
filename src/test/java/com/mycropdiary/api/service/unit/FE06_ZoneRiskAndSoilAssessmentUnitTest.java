package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.risk.CreateZoneRiskReq;
import com.mycropdiary.api.dto.risk.UpdateRiskControlReq;
import com.mycropdiary.api.dto.risk.ZoneRiskResp;
import com.mycropdiary.api.dto.soil.CreateSoilAssessmentReq;
import com.mycropdiary.api.dto.soil.SoilAssessmentResp;
import com.mycropdiary.api.entity.*;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.SoilAssessmentRepository;
import com.mycropdiary.api.repository.ZoneRiskAssessmentRepository;
import com.mycropdiary.api.service.impl.SoilAssessmentServiceImpl;
import com.mycropdiary.api.service.impl.ZoneRiskServiceImpl;
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
@DisplayName("Zone Risk & Soil Assessment Service Unit Tests")
public class FE06_ZoneRiskAndSoilAssessmentUnitTest {

    @Mock
    private ZoneRiskAssessmentRepository riskRepository;
    @Mock
    private SoilAssessmentRepository soilRepository;
    @Mock
    private FarmMemberRepository farmMemberRepository;
    @Mock
    private FarmAccessGuard accessGuard;

    @InjectMocks
    private ZoneRiskServiceImpl riskService;

    @InjectMocks
    private SoilAssessmentServiceImpl soilService;

    private Farm farm;
    private Zone zone;
    private Block block;
    private FarmMember assessorMember;

    @BeforeEach
    void setUp() {
        farm = new Farm();
        farm.setId(1);

        zone = new Zone();
        zone.setId(10);
        zone.setFarm(farm);
        zone.setZoneName("Zone A");

        block = new Block();
        block.setId(20);
        block.setZone(zone);
        block.setBlockName("Block A1");

        User user = new User("farmer@test.com", "hash", "Test Farmer", "0900000000", "user", "active");
        user.setId(5);

        assessorMember = new FarmMember(farm, user, "owner", "active");
        assessorMember.setId(2);
    }

    @Test
    @DisplayName("Record Zone Production Risk: Should succeed with valid inputs")
    void recordZoneRisk_validInputs_shouldSucceed() {
        when(accessGuard.requireZoneAccess(5, 10)).thenReturn(zone);
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1, 5, "active"))
                .thenReturn(Optional.of(assessorMember));

        ZoneRiskAssessment saved = new ZoneRiskAssessment(
                zone, assessorMember, "Chemical Drift", "medium",
                "Neighboring farm sprayed pesticides",
                "Plant windbreak barrier trees",
                null, Instant.now()
        );
        saved.setId(101);
        when(riskRepository.save(any(ZoneRiskAssessment.class))).thenReturn(saved);

        CreateZoneRiskReq req = new CreateZoneRiskReq(
                "Chemical Drift", "medium",
                "Neighboring farm sprayed pesticides",
                "Plant windbreak barrier trees",
                null, null
        );

        ZoneRiskResp resp = riskService.recordZoneRisk(5, 10, req);
        assertNotNull(resp);
        assertEquals(101, resp.riskId());
        assertEquals("medium", resp.riskLevel());
        assertEquals("Plant windbreak barrier trees", resp.mitigationPlan());
    }

    @Test
    @DisplayName("Record Zone Production Risk: Should reject invalid risk level")
    void recordZoneRisk_invalidLevel_shouldThrowBadRequest() {
        when(accessGuard.requireZoneAccess(5, 10)).thenReturn(zone);

        CreateZoneRiskReq req = new CreateZoneRiskReq(
                "Chemical Drift", "EXTREME_DANGER",
                "Description", "Mitigation", null, null
        );

        assertThrows(BadRequestException.class, () -> riskService.recordZoneRisk(5, 10, req));
    }

    @Test
    @DisplayName("Update Risk Control Measures: Should update mitigation plan")
    void updateRiskControlMeasures_validInputs_shouldSucceed() {
        ZoneRiskAssessment existing = new ZoneRiskAssessment(
                zone, assessorMember, "Chemical Drift", "medium",
                "Desc", "Old mitigation", null, Instant.now()
        );
        existing.setId(101);
        when(riskRepository.findById(101)).thenReturn(Optional.of(existing));
        when(accessGuard.requireZoneAccess(5, 10)).thenReturn(zone);
        when(riskRepository.save(any(ZoneRiskAssessment.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateRiskControlReq req = new UpdateRiskControlReq(
                "Upgraded 3m vegetative tree fence",
                "low", "Reduced risk after fence completion", null
        );

        ZoneRiskResp updated = riskService.updateRiskControlMeasures(5, 101, req);
        assertNotNull(updated);
        assertEquals("Upgraded 3m vegetative tree fence", updated.mitigationPlan());
        assertEquals("low", updated.riskLevel());
    }

    @Test
    @DisplayName("Create Soil Assessment: Should succeed with valid pH and moisture")
    void createSoilAssessment_validInputs_shouldSucceed() {
        when(accessGuard.requireBlockAccess(5, 20)).thenReturn(block);
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1, 5, "active"))
                .thenReturn(Optional.of(assessorMember));

        SoilAssessment saved = new SoilAssessment(
                block, assessorMember, new BigDecimal("6.50"), new BigDecimal("70.00"),
                "{\"nitrogen\":\"optimal\"}", "Soil sample is fertile", null, Instant.now()
        );
        saved.setId(201);
        when(soilRepository.save(any(SoilAssessment.class))).thenReturn(saved);

        CreateSoilAssessmentReq req = new CreateSoilAssessmentReq(
                new BigDecimal("6.50"), new BigDecimal("70.00"),
                "{\"nitrogen\":\"optimal\"}", "Soil sample is fertile", null, null
        );

        SoilAssessmentResp resp = soilService.createSoilAssessment(5, 20, req);
        assertNotNull(resp);
        assertEquals(201, resp.assessmentId());
        assertEquals(new BigDecimal("6.50"), resp.soilPh());
    }

    @Test
    @DisplayName("Create Soil Assessment: Should reject out-of-range pH value")
    void createSoilAssessment_outOfRangePh_shouldThrowBadRequest() {
        when(accessGuard.requireBlockAccess(5, 20)).thenReturn(block);

        CreateSoilAssessmentReq req = new CreateSoilAssessmentReq(
                new BigDecimal("15.50"), new BigDecimal("70.00"),
                null, null, null, null
        );

        assertThrows(BadRequestException.class, () -> soilService.createSoilAssessment(5, 20, req));
    }
}
