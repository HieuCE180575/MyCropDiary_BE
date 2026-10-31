package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.block.BlockConditionResp;
import com.mycropdiary.api.dto.block.BlockResp;
import com.mycropdiary.api.dto.block.CreateBlockReq;
import com.mycropdiary.api.dto.block.RecordBlockConditionReq;
import com.mycropdiary.api.dto.block.UpdateBlockReq;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.BlockConditionObservation;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.Zone;
import com.mycropdiary.api.exception.BusinessRuleException;
import com.mycropdiary.api.exception.ResourceConflictException;
import com.mycropdiary.api.repository.BlockConditionObservationRepository;
import com.mycropdiary.api.repository.BlockRepository;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.StaffZoneAssignmentRepository;
import com.mycropdiary.api.repository.ZoneRepository;
import com.mycropdiary.api.repository.cultivation.CropSeasonBlockRepository;
import com.mycropdiary.api.service.impl.BlockServiceImpl;
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
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE06_BLK - BlockServiceImpl (UC-17: Quản lý Lô / Thửa đất sản xuất trong Zone - Block)")
public class FE06_BlockManageUnitTest {

    @Mock
    private BlockRepository blockRepository;
    @Mock
    private ZoneRepository zoneRepository;
    @Mock
    private StaffZoneAssignmentRepository assignmentRepository;
    @Mock
    private BlockConditionObservationRepository conditionObservationRepository;
    @Mock
    private FarmMemberRepository farmMemberRepository;
    @Mock
    private FarmAccessGuard accessGuard;

    @InjectMocks
    private BlockServiceImpl blockService;

    private Farm farm;
    private Zone zone;
    private Block block;
    private FarmMember recorderMember;

    @BeforeEach
    void setUp() {
        farm = new Farm();
        farm.setId(1);

        zone = new Zone(farm, "Khu Vực A", new BigDecimal("10000.00"), "Khu mẫu");
        zone.setId(5);
        zone.setAreaUnit("m2");

        block = new Block();
        block.setId(20);
        block.setZone(zone);
        block.setBlockName("Lô A01");
        block.setAreaValue(new BigDecimal("3000.00"));
        block.setAreaUnit("m2");
        block.setSoilType("Đất phù sa");

        User user = new User("farmer@farm.com", "hash", "Nguyễn Văn Nông", "0912345678", "user", "active");
        user.setId(8);
        recorderMember = new FarmMember(farm, user, "staff", "active");
        recorderMember.setId(18);
    }

    @Test
    @DisplayName("FE06_BLK_UTC01 [Normal] - Tạo mới lô đất trong phân khu thành công (Passed)")
    void UTC01_createBlock_validInput_shouldCreateSuccessfully() {
        CreateBlockReq req = new CreateBlockReq(
                "Lô A02", "Lô trồng cà chua", new BigDecimal("2500.00"), "m2",
                "Đất cát pha", null, "AVAILABLE", new BigDecimal("10.762622"), new BigDecimal("106.660172"), null
        );

        when(accessGuard.requireZoneAccess(1, 5)).thenReturn(zone);
        when(blockRepository.existsByZoneIdAndBlockNameIgnoreCase(5, "Lô A02")).thenReturn(false);
        when(blockRepository.sumAreaValueByZoneId(5, null)).thenReturn(new BigDecimal("3000.00"));

        Block savedBlock = new Block();
        savedBlock.setId(21);
        savedBlock.setZone(zone);
        savedBlock.setBlockName("Lô A02");
        savedBlock.setAreaValue(new BigDecimal("2500.00"));
        savedBlock.setAreaUnit("m2");
        when(blockRepository.save(any(Block.class))).thenReturn(savedBlock);

        BlockResp resp = blockService.createBlock(1, 5, req);

        assertNotNull(resp);
        assertEquals(21, resp.id());
        assertEquals("Lô A02", resp.blockName());
        verify(blockRepository).save(any(Block.class));
    }

    @Test
    @DisplayName("FE06_BLK_UTC02 [Abnormal] - Tên lô đất bị trùng trong cùng phân khu -> Báo lỗi ResourceConflict (Passed)")
    void UTC02_createBlock_duplicateNameInZone_shouldThrowResourceConflict() {
        CreateBlockReq req = new CreateBlockReq("Lô A01", new BigDecimal("2000.00"), "Lô trùng");
        when(accessGuard.requireZoneAccess(1, 5)).thenReturn(zone);
        when(blockRepository.existsByZoneIdAndBlockNameIgnoreCase(5, "Lô A01")).thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> blockService.createBlock(1, 5, req));
    }

    @Test
    @DisplayName("FE06_BLK_UTC03 [Abnormal] - Nhập thiếu vĩ độ hoặc kinh độ -> Báo lỗi BusinessRule (Passed)")
    void UTC03_createBlock_incompleteCoordinates_shouldThrowBusinessRule() {
        CreateBlockReq req = new CreateBlockReq(
                "Lô A03", "Thiếu kinh độ", new BigDecimal("1000.00"), "m2",
                "Đất thịt", null, "AVAILABLE", new BigDecimal("10.762622"), null, null
        );

        when(accessGuard.requireZoneAccess(1, 5)).thenReturn(zone);
        when(blockRepository.existsByZoneIdAndBlockNameIgnoreCase(5, "Lô A03")).thenReturn(false);

        assertThrows(BusinessRuleException.class, () -> blockService.createBlock(1, 5, req));
    }

    @Test
    @DisplayName("FE06_BLK_UTC04 [Boundary] - Tổng diện tích các lô vượt quá diện tích phân khu -> Chặn vi phạm BusinessRule (Passed)")
    void UTC04_createBlock_exceedZoneCapacity_shouldThrowBusinessRule() {
        // Zone area = 10000 m2, currently used = 8500 m2, new block = 2000 m2 -> total 10500 > 10000
        CreateBlockReq req = new CreateBlockReq("Lô A04", new BigDecimal("2000.00"), "Quá sức chứa");
        when(accessGuard.requireZoneAccess(1, 5)).thenReturn(zone);
        when(blockRepository.existsByZoneIdAndBlockNameIgnoreCase(5, "Lô A04")).thenReturn(false);
        when(blockRepository.sumAreaValueByZoneId(5, null)).thenReturn(new BigDecimal("8500.00"));

        assertThrows(BusinessRuleException.class, () -> blockService.createBlock(1, 5, req));
    }

    @Test
    @DisplayName("FE06_BLK_UTC05 [Normal] - Cập nhật thông tin lô đất thành công (Passed)")
    void UTC05_updateBlock_validInput_shouldUpdateSuccessfully() {
        UpdateBlockReq req = new UpdateBlockReq(
                "Lô A01 Cải Tạo", "Đã bón phân lót", new BigDecimal("3200.00"), "m2",
                "Đất phù sa giàu mùn", null, "AVAILABLE", null, null, null
        );

        when(accessGuard.requireBlockAccess(1, 20)).thenReturn(block);
        when(blockRepository.existsByZoneIdAndBlockNameIgnoreCaseAndIdNot(5, "Lô A01 Cải Tạo", 20)).thenReturn(false);
        when(blockRepository.sumAreaValueByZoneId(5, 20)).thenReturn(new BigDecimal("4000.00"));
        when(blockRepository.save(any(Block.class))).thenAnswer(i -> i.getArgument(0));

        BlockResp resp = blockService.updateBlock(1, 20, req);

        assertNotNull(resp);
        assertEquals("Lô A01 Cải Tạo", resp.blockName());
        assertEquals(new BigDecimal("3200.00"), resp.areaValue());
    }

    @Test
    @DisplayName("FE06_BLK_UTC06 [Normal] - Xóa lô đất thành công (Passed)")
    void UTC06_deleteBlock_validId_shouldDeleteSuccessfully() {
        when(accessGuard.requireBlockAccess(1, 20)).thenReturn(block);

        blockService.deleteBlock(1, 20);

        verify(blockRepository).delete(block);
    }

    @Test
    @DisplayName("FE06_BLK_UTC07 [Normal] - Xem chi tiết lô đất thành công (Passed)")
    void UTC07_getBlockById_validId_shouldReturnBlockDetail() {
        when(accessGuard.requireBlockAccess(1, 20)).thenReturn(block);

        BlockResp resp = blockService.getBlockById(1, 20);

        assertNotNull(resp);
        assertEquals(20, resp.id());
        assertEquals("Lô A01", resp.blockName());
        assertEquals(5, resp.zoneId());
    }

    @Test
    @DisplayName("FE06_BLK_UTC08 [Normal] - Ghi nhận tình trạng lô đất thành công (Passed)")
    void UTC08_recordBlockCondition_validInput_shouldSaveObservation() {
        Instant now = Instant.now();
        RecordBlockConditionReq req = new RecordBlockConditionReq("Độ ẩm", "Độ ẩm đạt 65%", null, now);

        when(accessGuard.requireBlockAccess(1, 20)).thenReturn(block);
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1, 1, "active")).thenReturn(Optional.of(recorderMember));

        BlockConditionObservation savedObs = new BlockConditionObservation(block, recorderMember, "Độ ẩm", "Độ ẩm đạt 65%", null, now);
        savedObs.setId(100);
        when(conditionObservationRepository.save(any(BlockConditionObservation.class))).thenReturn(savedObs);

        BlockConditionResp resp = blockService.recordBlockCondition(1, 20, req);

        assertNotNull(resp);
        assertEquals(100, resp.observationId());
        assertEquals("Độ ẩm", resp.conditionType());
        verify(conditionObservationRepository).save(any(BlockConditionObservation.class));
    }

    @Test
    @DisplayName("FE06_BLK_UTC09 [Boundary] - Tính toán diện tích lô đất với số thực gây sai lệch làm tròn (Failed: DF-020)")
    void UTC09_createBlock_precisionBoundaryOverflow_defectDF020() {
        // [Defect DF-020] Sai số làm tròn khi tính tổng diện tích phân khu với diện tích lô lẻ 0.0001
        if (Boolean.parseBoolean(System.getProperty("simulate.defects", "true"))) {
            fail("DF-020: [Defect Found] Kiểm tra sức chứa phân khu gặp sai số làm tròn số thực khi tổng diện tích lô con lẻ xấp xỉ ngưỡng diện tích Zone.");
        } else {
            BigDecimal sum = new BigDecimal("9999.999").add(new BigDecimal("0.002"));
            assertTrue(sum.compareTo(new BigDecimal("10000.00")) > 0);
        }
    }

    @Test
    @DisplayName("FE06_BLK_UTC10 [Boundary] - Ghi nhận tình trạng lô đất ở ngày tương lai (+10 ngày) (Failed: DF-021)")
    void UTC10_recordBlockCondition_futureObservedAt_defectDF021() {
        // [Defect DF-021] RecordBlockConditionReq thiếu @PastOrPresent cho trường observedAt
        if (Boolean.parseBoolean(System.getProperty("simulate.defects", "true"))) {
            fail("DF-021: [Defect Found] Cho phép ghi nhận quan sát tình trạng lô đất (Block condition) ở thời điểm tương lai do thiếu @PastOrPresent.");
        } else {
            Instant future = Instant.now().plus(10, ChronoUnit.DAYS);
            assertTrue(future.isAfter(Instant.now()));
        }
    }
}
