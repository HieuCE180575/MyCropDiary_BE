package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.block.CreateBlockReqDTO;
import com.mycropdiary.api.dto.farm.CreateZoneReqDTO;
import com.mycropdiary.api.dto.farm.UpdateZoneReqDTO;
import com.mycropdiary.api.dto.farm.ZoneRespDTO;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.Zone;
import com.mycropdiary.api.exception.BusinessRuleException;
import com.mycropdiary.api.repository.BlockConditionRepository;
import com.mycropdiary.api.repository.BlockRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.StaffZoneAssignmentRepository;
import com.mycropdiary.api.repository.ZoneRepository;
import com.mycropdiary.api.repository.cultivation.CropSeasonBlockRepository;
import com.mycropdiary.api.service.ZoneAssignmentService;
import com.mycropdiary.api.service.impl.BlockServiceImpl;
import com.mycropdiary.api.service.impl.ZoneServiceImpl;
import com.mycropdiary.api.util.FarmAccessGuard;
import com.mycropdiary.api.util.GeoAreaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE06_GEO - GeoAreaUtils & Multi-Unit (ha/m²) Farm-Zone-Block Area Capacity Unit Tests (UC-16, UC-17)")
public class FE06_GeoAreaAndCapacityUnitTest {

    // ~10,000 m² (1 ha) polygon near Can Tho
    private static final String FARM_1HA_OBJ_POLYGON =
            "[{\"lat\":10.0,\"lng\":105.78},{\"lat\":10.0009,\"lng\":105.78},{\"lat\":10.0009,\"lng\":105.78091},{\"lat\":10.0,\"lng\":105.78091}]";
    private static final String FARM_1HA_ARR_POLYGON =
            "[[10.0,105.78],[10.0009,105.78],[10.0009,105.78091],[10.0,105.78091]]";

    @Mock private ZoneRepository zoneRepository;
    @Mock private FarmRepository farmRepository;
    @Mock private BlockRepository blockRepository;
    @Mock private BlockConditionRepository blockConditionRepository;
    @Mock private StaffZoneAssignmentRepository staffZoneAssignmentRepository;
    @Mock private ZoneAssignmentService zoneAssignmentService;
    @Mock private CropSeasonBlockRepository cropSeasonBlockRepository;
    @Mock private FarmAccessGuard accessGuard;

    @InjectMocks private ZoneServiceImpl zoneService;
    @InjectMocks private BlockServiceImpl blockService;

    private Farm farmWith1HaPolygon;
    private Zone zone5000m2;

    @BeforeEach
    void setUp() {
        farmWith1HaPolygon = new Farm();
        farmWith1HaPolygon.setId(1);
        farmWith1HaPolygon.setFarmName("Nông trại 1 Hecta");
        farmWith1HaPolygon.setBoundaryCoordinates(FARM_1HA_OBJ_POLYGON);

        zone5000m2 = new Zone(farmWith1HaPolygon, "Phân khu A", new BigDecimal("5000"), "5000 m²");
        zone5000m2.setId(10);
        zone5000m2.setAreaUnit("m2");
    }

    @Test
    @DisplayName("FE06_GEO_UTC01 [Normal] - GeoAreaUtils.toSquareMeters quy đổi chuẩn ha -> m², giữ nguyên m² và xử lý null")
    void UTC01_toSquareMeters_convertsHaM2AndNull() {
        assertEquals(0, new BigDecimal("15000").compareTo(GeoAreaUtils.toSquareMeters(new BigDecimal("1.5"), "ha")));
        assertEquals(0, new BigDecimal("15000").compareTo(GeoAreaUtils.toSquareMeters(new BigDecimal("1.5"), " HA ")));
        assertEquals(0, new BigDecimal("500").compareTo(GeoAreaUtils.toSquareMeters(new BigDecimal("500"), "m2")));
        assertEquals(0, BigDecimal.ZERO.compareTo(GeoAreaUtils.toSquareMeters(null, "ha")));
    }

    @Test
    @DisplayName("FE06_GEO_UTC02 [Normal] - GeoAreaUtils.calculatePolygonAreaM2 tính diện tích tương đương cho cả 2 định dạng JSON object & array")
    void UTC02_calculatePolygonAreaM2_supportsObjectAndArrayFormats() {
        double areaObj = GeoAreaUtils.calculatePolygonAreaM2(FARM_1HA_OBJ_POLYGON);
        double areaArr = GeoAreaUtils.calculatePolygonAreaM2(FARM_1HA_ARR_POLYGON);

        assertTrue(areaObj > 9500 && areaObj < 10500, "Expected ~10,000 m² but got " + areaObj);
        assertEquals(areaObj, areaArr, 1.0);
    }

    @Test
    @DisplayName("FE06_GEO_UTC03 [Boundary] - GeoAreaUtils.calculatePolygonAreaM2 trả về 0.0 khi null, rỗng, < 3 đỉnh hoặc JSON lỗi")
    void UTC03_calculatePolygonAreaM2_edgeAndMalformedInputsReturnZero() {
        assertEquals(0.0, GeoAreaUtils.calculatePolygonAreaM2(null));
        assertEquals(0.0, GeoAreaUtils.calculatePolygonAreaM2("   "));
        assertEquals(0.0, GeoAreaUtils.calculatePolygonAreaM2("[[10.0,105.78],[10.001,105.78]]"));
        assertEquals(0.0, GeoAreaUtils.calculatePolygonAreaM2("{invalid-json"));
    }

    @Test
    @DisplayName("FE06_GEO_UTC04 [Abnormal] - Tạo phân khu 2 ha (20,000 m²) vượt quá diện tích ranh giới trang trại (~10,000 m²) -> Ném BusinessRuleException")
    void UTC04_createZone_exceedsFarmPolygonArea_throwsBusinessRule() {
        CreateZoneReqDTO req = new CreateZoneReqDTO("Phân khu Quá Lớn", new BigDecimal("2.0"), "ha", "Vượt biên", null);
        when(farmRepository.findById(1)).thenReturn(Optional.of(farmWith1HaPolygon));
        when(zoneRepository.existsByFarmIdAndZoneNameIgnoreCase(1, "Phân khu Quá Lớn")).thenReturn(false);
        when(zoneRepository.findByFarmId(1)).thenReturn(List.of());

        assertThrows(BusinessRuleException.class, () -> zoneService.createZone(1, 1, req));
        verify(zoneRepository, never()).save(any());
    }

    @Test
    @DisplayName("FE06_GEO_UTC05 [Normal] - Tạo phân khu 0.4 ha (4,000 m²) kết hợp phân khu cũ 5,000 m² nằm gọn trong trang trại ~10,000 m² -> Thành công")
    void UTC05_createZone_fitsWithinFarmPolygonArea_succeeds() {
        CreateZoneReqDTO req = new CreateZoneReqDTO("Phân khu B", new BigDecimal("0.4"), "ha", "Hợp lệ", null);
        when(farmRepository.findById(1)).thenReturn(Optional.of(farmWith1HaPolygon));
        when(zoneRepository.existsByFarmIdAndZoneNameIgnoreCase(1, "Phân khu B")).thenReturn(false);
        when(zoneRepository.findByFarmId(1)).thenReturn(List.of(zone5000m2));
        when(zoneRepository.save(any(Zone.class))).thenAnswer(i -> {
            Zone z = i.getArgument(0);
            z.setId(11);
            return z;
        });

        ZoneRespDTO resp = zoneService.createZone(1, 1, req);

        assertNotNull(resp);
        assertEquals(11, resp.id());
        assertEquals("ha", resp.areaUnit());
    }

    @Test
    @DisplayName("FE06_GEO_UTC06 [Boundary] - Thu nhỏ phân khu xuống 0.3 ha (3,000 m²) nhỏ hơn tổng các lô đất con (4,000 m²) -> Ném BusinessRuleException")
    void UTC06_updateZone_shrunkBelowExistingBlocksCrossUnit_throwsBusinessRule() {
        Block block4000 = new Block();
        block4000.setId(101);
        block4000.setAreaValue(new BigDecimal("4000"));
        block4000.setAreaUnit("m2");

        UpdateZoneReqDTO req = new UpdateZoneReqDTO("Phân khu A", new BigDecimal("0.3"), "ha", "Thu nhỏ quá mức", null);
        when(accessGuard.requireZoneAccess(1, 10)).thenReturn(zone5000m2);
        when(zoneRepository.existsByFarmIdAndZoneNameIgnoreCaseAndIdNot(1, "Phân khu A", 10)).thenReturn(false);
        when(zoneRepository.findByFarmId(1)).thenReturn(List.of(zone5000m2));
        when(blockRepository.findByZoneId(10)).thenReturn(List.of(block4000));

        assertThrows(BusinessRuleException.class, () -> zoneService.updateZone(1, 10, req));
        verify(zoneRepository, never()).save(any());
    }

    @Test
    @DisplayName("FE06_GEO_UTC07 [Abnormal] - Tạo lô đất 0.6 ha (6,000 m²) vượt quá sức chứa phân khu 5,000 m² sau quy đổi đơn vị -> Ném BusinessRuleException")
    void UTC07_createBlock_haExceedsZoneM2Capacity_throwsBusinessRule() {
        CreateBlockReqDTO req = new CreateBlockReqDTO(
                "Lô Lớn", "Mô tả", new BigDecimal("0.6"), "ha", "Đất phù sa", null, "active", null, null, null
        );
        when(accessGuard.requireZoneAccess(1, 10)).thenReturn(zone5000m2);
        when(blockRepository.existsByZoneIdAndBlockNameIgnoreCase(10, "Lô Lớn")).thenReturn(false);

        assertThrows(BusinessRuleException.class, () -> blockService.createBlock(1, 10, req));
        verify(blockRepository, never()).save(any());
    }

    @Test
    @DisplayName("FE06_GEO_UTC08 [Boundary] - Tạo lô đất diện tích bằng 0 m² -> Chặn bằng BusinessRuleException")
    void UTC08_createBlock_zeroArea_throwsBusinessRule() {
        CreateBlockReqDTO req = new CreateBlockReqDTO(
                "Lô Rỗng", "Mô tả", BigDecimal.ZERO, "m2", "Đất phù sa", null, "active", null, null, null
        );
        when(accessGuard.requireZoneAccess(1, 10)).thenReturn(zone5000m2);
        when(blockRepository.existsByZoneIdAndBlockNameIgnoreCase(10, "Lô Rỗng")).thenReturn(false);

        assertThrows(BusinessRuleException.class, () -> blockService.createBlock(1, 10, req));
        verify(blockRepository, never()).save(any());
    }
}
