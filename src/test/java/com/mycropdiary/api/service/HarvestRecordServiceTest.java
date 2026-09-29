package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.cultivation.CreateHarvestRecordRequest;
import com.mycropdiary.api.dto.cultivation.HarvestRecordResponse;
import com.mycropdiary.api.entity.AppUser;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.ProductionArea;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import com.mycropdiary.api.entity.cultivation.HarvestRecord;
import com.mycropdiary.api.entity.cultivation.Plot;

import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ForbiddenException;
import com.mycropdiary.api.exception.ResourceNotFoundException;

import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.StaffAreaAssignmentRepository;
import com.mycropdiary.api.repository.cultivation.CropSeasonRepository;
import com.mycropdiary.api.repository.cultivation.HarvestRecordRepository;

import com.mycropdiary.api.service.impl.HarvestRecordServiceImpl;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HarvestRecordServiceTest {

    @Mock
    private HarvestRecordRepository harvestRecordRepository;
    @Mock
    private CropSeasonRepository cropSeasonRepository;
    @Mock
    private FarmMemberRepository farmMemberRepository;
    @Mock
    private StaffAreaAssignmentRepository staffAreaAssignmentRepository;

    @InjectMocks
    private HarvestRecordServiceImpl harvestRecordService;

    private Farm farm;
    private ProductionArea areaA;
    private Plot plotA;
    private CropSeason activeSeason;
    private CropSeason completedSeason;
    private AppUser ownerUser;
    private AppUser staffUser;
    private FarmMember ownerMember;
    private FarmMember staffMember;

    @BeforeEach
    void setUp() {
        farm = new Farm();
        farm.setId(1L);
        farm.setFarmName("Nông trại Việt");

        areaA = new ProductionArea();
        areaA.setId(10L);
        areaA.setFarm(farm);

        plotA = new Plot();
        plotA.setId(100L);
        plotA.setProductionArea(areaA);

        ownerUser = new AppUser();
        ownerUser.setId(1L);
        ownerUser.setFullName("Chủ Nông Trại");

        staffUser = new AppUser();
        staffUser.setId(2L);
        staffUser.setFullName("Nhân Viên Trại");

        ownerMember = new FarmMember();
        ownerMember.setId(101L);
        ownerMember.setFarm(farm);
        ownerMember.setUser(ownerUser);
        ownerMember.setFarmRole("OWNER");
        ownerMember.setStatus("ACTIVE");

        staffMember = new FarmMember();
        staffMember.setId(102L);
        staffMember.setFarm(farm);
        staffMember.setUser(staffUser);
        staffMember.setFarmRole("STAFF");
        staffMember.setStatus("ACTIVE");

        activeSeason = new CropSeason();
        activeSeason.setId(50L);
        activeSeason.setFarm(farm);
        activeSeason.setPlot(plotA);
        activeSeason.setStatus("ACTIVE");

        completedSeason = new CropSeason();
        completedSeason.setId(51L);
        completedSeason.setFarm(farm);
        completedSeason.setPlot(plotA);
        completedSeason.setStatus("COMPLETED");
    }

    @Test
    @DisplayName("Tạo HarvestRecord thành công với vai trò OWNER")
    void createHarvestRecord_AsOwner_Success() {
        CreateHarvestRecordRequest request = new CreateHarvestRecordRequest(
                50L, "LOT-001", LocalDateTime.now(), new BigDecimal("150.500"), "kg", "Loại 1", "Kho A", "TR-001", "Ghi chú thu hoạch"
        );

        when(cropSeasonRepository.findById(50L)).thenReturn(Optional.of(activeSeason));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1L, 1L, "ACTIVE")).thenReturn(Optional.of(ownerMember));
        when(harvestRecordRepository.existsByHarvestLotCode("LOT-001")).thenReturn(false);
        when(harvestRecordRepository.existsByTraceabilityCode("TR-001")).thenReturn(false);

        HarvestRecord savedRecord = new HarvestRecord();
        savedRecord.setId(10L);
        savedRecord.setCropSeason(activeSeason);
        savedRecord.setRecordedByMember(ownerMember);
        savedRecord.setHarvestLotCode("LOT-001");
        savedRecord.setHarvestedAt(request.harvestedAt());
        savedRecord.setQuantity(request.quantity());
        savedRecord.setUnit("kg");

        when(harvestRecordRepository.save(any(HarvestRecord.class))).thenReturn(savedRecord);

        HarvestRecordResponse response = harvestRecordService.createHarvestRecord(1L, request);

        assertNotNull(response);
        assertEquals(10L, response.harvestRecordId());
        assertEquals("LOT-001", response.harvestLotCode());
        assertEquals(new BigDecimal("150.500"), response.quantity());
        verify(harvestRecordRepository).save(any(HarvestRecord.class));
    }

    @Test
    @DisplayName("Tạo HarvestRecord thành công với STAFF được assign đúng ProductionArea")
    void createHarvestRecord_AsStaff_AssignedToArea_Success() {
        CreateHarvestRecordRequest request = new CreateHarvestRecordRequest(
                50L, null, LocalDateTime.now(), new BigDecimal("100.000"), "kg", null, null, null, null
        );

        when(cropSeasonRepository.findById(50L)).thenReturn(Optional.of(activeSeason));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1L, 2L, "ACTIVE")).thenReturn(Optional.of(staffMember));
        when(staffAreaAssignmentRepository.existsByFarmMemberIdAndProductionAreaIdAndActiveTrue(102L, 10L)).thenReturn(true);

        HarvestRecord savedRecord = new HarvestRecord();
        savedRecord.setId(11L);
        savedRecord.setCropSeason(activeSeason);
        savedRecord.setRecordedByMember(staffMember);
        savedRecord.setHarvestLotCode("HRV-S50-12345");

        when(harvestRecordRepository.save(any(HarvestRecord.class))).thenReturn(savedRecord);

        HarvestRecordResponse response = harvestRecordService.createHarvestRecord(2L, request);

        assertNotNull(response);
        assertEquals(11L, response.harvestRecordId());
    }

    @Test
    @DisplayName("Tạo HarvestRecord thất bại nếu STAFF không được assign ProductionArea")
    void createHarvestRecord_AsStaff_NotAssigned_ThrowsForbidden() {
        CreateHarvestRecordRequest request = new CreateHarvestRecordRequest(
                50L, null, LocalDateTime.now(), new BigDecimal("100.000"), "kg", null, null, null, null
        );

        when(cropSeasonRepository.findById(50L)).thenReturn(Optional.of(activeSeason));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1L, 2L, "ACTIVE")).thenReturn(Optional.of(staffMember));
        when(staffAreaAssignmentRepository.existsByFarmMemberIdAndProductionAreaIdAndActiveTrue(102L, 10L)).thenReturn(false);

        ForbiddenException exception = assertThrows(ForbiddenException.class,
                () -> harvestRecordService.createHarvestRecord(2L, request));

        assertTrue(exception.getMessage().contains("Nhân viên chưa được phân công"));
        verify(harvestRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tạo HarvestRecord thất bại nếu Quantity <= 0")
    void createHarvestRecord_QuantityInvalid_ThrowsBadRequest() {
        CreateHarvestRecordRequest request = new CreateHarvestRecordRequest(
                50L, null, LocalDateTime.now(), new BigDecimal("0.000"), "kg", null, null, null, null
        );

        when(cropSeasonRepository.findById(50L)).thenReturn(Optional.of(activeSeason));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1L, 1L, "ACTIVE")).thenReturn(Optional.of(ownerMember));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> harvestRecordService.createHarvestRecord(1L, request));

        assertTrue(exception.getMessage().contains("phải lớn hơn 0"));
    }

    @Test
    @DisplayName("Tạo HarvestRecord thất bại nếu HarvestLotCode bị trùng")
    void createHarvestRecord_DuplicateLotCode_ThrowsBadRequest() {
        CreateHarvestRecordRequest request = new CreateHarvestRecordRequest(
                50L, "DUP-LOT", LocalDateTime.now(), new BigDecimal("10.000"), "kg", null, null, null, null
        );

        when(cropSeasonRepository.findById(50L)).thenReturn(Optional.of(activeSeason));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1L, 1L, "ACTIVE")).thenReturn(Optional.of(ownerMember));
        when(harvestRecordRepository.existsByHarvestLotCode("DUP-LOT")).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> harvestRecordService.createHarvestRecord(1L, request));

        assertTrue(exception.getMessage().contains("HarvestLotCode"));
    }

    @Test
    @DisplayName("Tạo HarvestRecord thất bại khi CropSeason đã COMPLETED")
    void createHarvestRecord_ClosedSeason_ThrowsBadRequest() {
        CreateHarvestRecordRequest request = new CreateHarvestRecordRequest(
                51L, null, LocalDateTime.now(), new BigDecimal("10.000"), "kg", null, null, null, null
        );

        when(cropSeasonRepository.findById(51L)).thenReturn(Optional.of(completedSeason));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1L, 1L, "ACTIVE")).thenReturn(Optional.of(ownerMember));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> harvestRecordService.createHarvestRecord(1L, request));

        assertTrue(exception.getMessage().contains("mùa vụ đã đóng"));
    }

    @Test
    @DisplayName("Xem danh sách HarvestRecord theo Season có phân trang thành công")
    void getHarvestRecordsBySeason_Success() {
        HarvestRecord record = new HarvestRecord();
        record.setId(10L);
        record.setCropSeason(activeSeason);
        record.setRecordedByMember(ownerMember);

        Pageable pageable = PageRequest.of(0, 20);

        when(cropSeasonRepository.findById(50L)).thenReturn(Optional.of(activeSeason));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1L, 1L, "ACTIVE")).thenReturn(Optional.of(ownerMember));
        when(harvestRecordRepository.findByCropSeasonId(50L, pageable)).thenReturn(new PageImpl<>(List.of(record)));

        PageResponse<HarvestRecordResponse> result = harvestRecordService.getHarvestRecordsBySeason(1L, 50L, pageable);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals(10L, result.content().get(0).harvestRecordId());
    }

    @Test
    @DisplayName("Xem chi tiết HarvestRecord theo ID thành công")
    void getHarvestRecordById_Success() {
        HarvestRecord record = new HarvestRecord();
        record.setId(10L);
        record.setCropSeason(activeSeason);
        record.setRecordedByMember(ownerMember);

        when(harvestRecordRepository.findById(10L)).thenReturn(Optional.of(record));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1L, 1L, "ACTIVE")).thenReturn(Optional.of(ownerMember));

        HarvestRecordResponse response = harvestRecordService.getHarvestRecordById(1L, 10L);

        assertNotNull(response);
        assertEquals(10L, response.harvestRecordId());
    }

    @Test
    @DisplayName("Xem chi tiết HarvestRecord thất bại khi không tồn tại")
    void getHarvestRecordById_NotFound_ThrowsNotFound() {
        when(harvestRecordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> harvestRecordService.getHarvestRecordById(1L, 99L));
    }
}
