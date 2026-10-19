package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.CreateHarvestRecordReq;
import com.mycropdiary.api.dto.cultivation.HarvestRecordResp;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.Zone;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import com.mycropdiary.api.entity.cultivation.HarvestRecord;

import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ForbiddenException;
import com.mycropdiary.api.exception.ResourceNotFoundException;

import com.mycropdiary.api.repository.FarmMemberRepository;
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

    @InjectMocks
    private HarvestRecordServiceImpl harvestRecordService;

    private Farm farm;
    private Zone zoneA;
    private Block blockA;
    private CropSeason activeSeason;
    private CropSeason completedSeason;
    private User ownerUser;
    private User staffUser;
    private FarmMember ownerMember;
    private FarmMember staffMember;

    private final Integer farmId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer zoneId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer blockId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer ownerUserId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer staffUserId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer ownerMemberId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer staffMemberId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer activeSeasonId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer completedSeasonId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer harvestRecordId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);

    @BeforeEach
    void setUp() {
        farm = new Farm();
        farm.setId(farmId);
        farm.setFarmName("Nông trại Việt");

        zoneA = new Zone();
        zoneA.setId(zoneId);
        zoneA.setFarm(farm);

        blockA = new Block();
        blockA.setId(blockId);
        blockA.setZone(zoneA);

        ownerUser = new User();
        ownerUser.setId(ownerUserId);
        ownerUser.setFullName("Chủ Nông Trại");

        staffUser = new User();
        staffUser.setId(staffUserId);
        staffUser.setFullName("Nhân Viên Trại");

        ownerMember = new FarmMember();
        ownerMember.setId(ownerMemberId);
        ownerMember.setFarm(farm);
        ownerMember.setUser(ownerUser);
        ownerMember.setMemberRole("owner");
        ownerMember.setMembershipStatus("active");

        staffMember = new FarmMember();
        staffMember.setId(staffMemberId);
        staffMember.setFarm(farm);
        staffMember.setUser(staffUser);
        staffMember.setMemberRole("member");
        staffMember.setMembershipStatus("active");

        activeSeason = new CropSeason();
        activeSeason.setId(activeSeasonId);
        activeSeason.setFarm(farm);
        activeSeason.setPlot(blockA);
        activeSeason.setSeasonStatus("active");

        completedSeason = new CropSeason();
        completedSeason.setId(completedSeasonId);
        completedSeason.setFarm(farm);
        completedSeason.setPlot(blockA);
        completedSeason.setSeasonStatus("completed");
    }

    @Test
    @DisplayName("Tạo HarvestRecord thành công với vai trò OWNER")
    void createHarvestRecord_AsOwner_Success() {
        CreateHarvestRecordReq request = new CreateHarvestRecordReq(
                activeSeasonId, "LOT-001", LocalDateTime.now(), new BigDecimal("150.500"), "kg", "Loại 1", "Kho A", "TR-001", "Ghi chú thu hoạch"
        );

        when(cropSeasonRepository.findById(activeSeasonId)).thenReturn(Optional.of(activeSeason));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, ownerUserId, "active")).thenReturn(Optional.of(ownerMember));

        HarvestRecord savedRecord = new HarvestRecord();
        savedRecord.setId(harvestRecordId);
        savedRecord.setFarm(farm);
        savedRecord.setCropSeason(activeSeason);
        savedRecord.setRecordedBy(ownerMember);
        savedRecord.setHarvestDate(request.harvestedAt().toLocalDate());
        savedRecord.setQuantity(request.quantity());
        savedRecord.setUnit("kg");

        when(harvestRecordRepository.save(any(HarvestRecord.class))).thenReturn(savedRecord);

        HarvestRecordResp response = harvestRecordService.createHarvestRecord(ownerUserId, request);

        assertNotNull(response);
        assertEquals(harvestRecordId, response.harvestRecordId());
        assertEquals(new BigDecimal("150.500"), response.quantity());
        verify(harvestRecordRepository).save(any(HarvestRecord.class));
    }

    @Test
    @DisplayName("Tạo HarvestRecord thất bại nếu không phải active member của farm")
    void createHarvestRecord_NotMember_ThrowsForbidden() {
        CreateHarvestRecordReq request = new CreateHarvestRecordReq(
                activeSeasonId, null, LocalDateTime.now(), new BigDecimal("100.000"), "kg", null, null, null, null
        );

        when(cropSeasonRepository.findById(activeSeasonId)).thenReturn(Optional.of(activeSeason));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, staffUserId, "active")).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class,
                () -> harvestRecordService.createHarvestRecord(staffUserId, request));
        verify(harvestRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tạo HarvestRecord thất bại nếu Quantity <= 0")
    void createHarvestRecord_QuantityInvalid_ThrowsBadRequest() {
        CreateHarvestRecordReq request = new CreateHarvestRecordReq(
                activeSeasonId, null, LocalDateTime.now(), new BigDecimal("0.000"), "kg", null, null, null, null
        );

        when(cropSeasonRepository.findById(activeSeasonId)).thenReturn(Optional.of(activeSeason));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, ownerUserId, "active")).thenReturn(Optional.of(ownerMember));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> harvestRecordService.createHarvestRecord(ownerUserId, request));

        assertTrue(exception.getMessage().contains("phải lớn hơn 0"));
    }

    @Test
    @DisplayName("Tạo HarvestRecord thất bại khi CropSeason đã COMPLETED")
    void createHarvestRecord_ClosedSeason_ThrowsBadRequest() {
        CreateHarvestRecordReq request = new CreateHarvestRecordReq(
                completedSeasonId, null, LocalDateTime.now(), new BigDecimal("10.000"), "kg", null, null, null, null
        );

        when(cropSeasonRepository.findById(completedSeasonId)).thenReturn(Optional.of(completedSeason));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, ownerUserId, "active")).thenReturn(Optional.of(ownerMember));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> harvestRecordService.createHarvestRecord(ownerUserId, request));

        assertTrue(exception.getMessage().contains("mùa vụ đã đóng"));
    }

    @Test
    @DisplayName("Xem danh sách HarvestRecord theo Season có phân trang thành công")
    void getHarvestRecordsBySeason_Success() {
        HarvestRecord record = new HarvestRecord();
        record.setId(harvestRecordId);
        record.setFarm(farm);
        record.setCropSeason(activeSeason);
        record.setRecordedBy(ownerMember);

        Pageable pageable = PageRequest.of(0, 20);

        when(cropSeasonRepository.findById(activeSeasonId)).thenReturn(Optional.of(activeSeason));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, ownerUserId, "active")).thenReturn(Optional.of(ownerMember));
        when(harvestRecordRepository.findByCropSeasonId(activeSeasonId, pageable)).thenReturn(new PageImpl<>(List.of(record)));

        PageResp<HarvestRecordResp> result = harvestRecordService.getHarvestRecordsBySeason(ownerUserId, activeSeasonId, pageable);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals(harvestRecordId, result.content().get(0).harvestRecordId());
    }

    @Test
    @DisplayName("Xem chi tiết HarvestRecord theo ID thành công")
    void getHarvestRecordById_Success() {
        HarvestRecord record = new HarvestRecord();
        record.setId(harvestRecordId);
        record.setFarm(farm);
        record.setCropSeason(activeSeason);
        record.setRecordedBy(ownerMember);

        when(harvestRecordRepository.findById(harvestRecordId)).thenReturn(Optional.of(record));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, ownerUserId, "active")).thenReturn(Optional.of(ownerMember));

        HarvestRecordResp response = harvestRecordService.getHarvestRecordById(ownerUserId, harvestRecordId);

        assertNotNull(response);
        assertEquals(harvestRecordId, response.harvestRecordId());
    }

    @Test
    @DisplayName("Xem chi tiết HarvestRecord thất bại khi không tồn tại")
    void getHarvestRecordById_NotFound_ThrowsNotFound() {
        Integer unknownId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
        when(harvestRecordRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> harvestRecordService.getHarvestRecordById(ownerUserId, unknownId));
    }
}
