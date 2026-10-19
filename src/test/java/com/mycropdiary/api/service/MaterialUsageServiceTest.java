package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.CreateMaterialUsageReq;
import com.mycropdiary.api.dto.cultivation.MaterialUsageResp;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.Zone;
import com.mycropdiary.api.entity.cultivation.*;

import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ForbiddenException;

import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.cultivation.*;

import com.mycropdiary.api.service.impl.MaterialUsageServiceImpl;

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
class MaterialUsageServiceTest {

    @Mock
    private MaterialUsageRepository materialUsageRepository;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private FarmingActivityRepository farmingActivityRepository;
    @Mock
    private FarmMemberRepository farmMemberRepository;

    @InjectMocks
    private MaterialUsageServiceImpl materialUsageService;

    private Farm farm;
    private Zone zoneA;
    private Block blockA;
    private CropSeason activeSeason;
    private CropSeason completedSeason;
    private FarmingActivity activity;
    private User ownerUser;
    private User staffUser;
    private FarmMember ownerMember;
    private FarmMember staffMember;
    private Material fertilizerMaterial;
    private Material pesticideMaterial;
    private Material inactiveMaterial;

    private final Integer farmId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer zoneId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer blockId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer ownerUserId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer staffUserId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer ownerMemberId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer staffMemberId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer activeSeasonId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer completedSeasonId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer activityId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer fertilizerId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer pesticideId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer inactiveMaterialId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
    private final Integer usageId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);

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

        activity = new FarmingActivity();
        activity.setId(activityId);
        activity.setFarm(farm);
        activity.setCropSeason(activeSeason);
        activity.setBlock(blockA);
        activity.setActivityName("Bón phân đợt 1");

        fertilizerMaterial = new Material(farm, "Phân NPK 16-16-8", "fertilizer", "kg");
        fertilizerMaterial.setId(fertilizerId);
        fertilizerMaterial.setIsActive(true);

        pesticideMaterial = new Material(farm, "Thuốc Trừ Sâu An Toàn", "pesticide", "chai");
        pesticideMaterial.setId(pesticideId);
        pesticideMaterial.setIsActive(true);

        inactiveMaterial = new Material(farm, "Vật tư cũ", "other", "cái");
        inactiveMaterial.setId(inactiveMaterialId);
        inactiveMaterial.setIsActive(false);
    }

    @Test
    @DisplayName("Tạo MaterialUsage thành công với vai trò OWNER")
    void createMaterialUsage_AsOwner_Success() {
        CreateMaterialUsageReq request = new CreateMaterialUsageReq(
                activityId, fertilizerId, null, LocalDateTime.now(), new BigDecimal("2.500"), "kg", "50g/gốc", "Rải gốc", null, "Thời tiết nắng"
        );

        when(farmingActivityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, ownerUserId, "active")).thenReturn(Optional.of(ownerMember));
        when(materialRepository.findById(fertilizerId)).thenReturn(Optional.of(fertilizerMaterial));

        MaterialUsage savedUsage = new MaterialUsage();
        savedUsage.setId(usageId);
        savedUsage.setFarm(farm);
        savedUsage.setFarmingActivity(activity);
        savedUsage.setMaterial(fertilizerMaterial);
        savedUsage.setUsedBy(ownerMember);
        savedUsage.setUsedAt(request.usedAt());
        savedUsage.setQuantity(request.quantity());

        when(materialUsageRepository.save(any(MaterialUsage.class))).thenReturn(savedUsage);

        MaterialUsageResp response = materialUsageService.createMaterialUsage(ownerUserId, request);

        assertNotNull(response);
        assertEquals(usageId, response.materialUsageId());
        assertEquals("Phân NPK 16-16-8", response.materialName());
        verify(materialUsageRepository).save(any(MaterialUsage.class));
    }

    @Test
    @DisplayName("Tạo MaterialUsage thất bại nếu không phải active member của farm")
    void createMaterialUsage_NotMember_ThrowsForbidden() {
        CreateMaterialUsageReq request = new CreateMaterialUsageReq(
                activityId, fertilizerId, null, LocalDateTime.now(), new BigDecimal("1.000"), "kg", null, null, null, null
        );

        when(farmingActivityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, staffUserId, "active")).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class,
                () -> materialUsageService.createMaterialUsage(staffUserId, request));
        verify(materialUsageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tạo MaterialUsage thất bại khi Unit trong request không khớp với Material.unit (kg vs L)")
    void createMaterialUsage_UnitMismatch_ThrowsBadRequest() {
        CreateMaterialUsageReq request = new CreateMaterialUsageReq(
                activityId, fertilizerId, null, LocalDateTime.now(), new BigDecimal("1.000"), "L", null, null, null, null
        );

        when(farmingActivityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, ownerUserId, "active")).thenReturn(Optional.of(ownerMember));
        when(materialRepository.findById(fertilizerId)).thenReturn(Optional.of(fertilizerMaterial));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> materialUsageService.createMaterialUsage(ownerUserId, request));

        assertTrue(exception.getMessage().contains("không khớp với đơn vị tính của vật tư"));
    }

    @Test
    @DisplayName("Tạo MaterialUsage thất bại nếu Material thuộc Farm khác")
    void createMaterialUsage_MaterialBelongsToDifferentFarm_ThrowsBadRequest() {
        Farm farmB = new Farm();
        farmB.setId(java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000));
        Integer otherMatId = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000);
        Material otherFarmMaterial = new Material(farmB, "Phân Farm B", "fertilizer", "kg");
        otherFarmMaterial.setId(otherMatId);

        CreateMaterialUsageReq request = new CreateMaterialUsageReq(
                activityId, otherMatId, null, LocalDateTime.now(), new BigDecimal("1.000"), "kg", null, null, null, null
        );

        when(farmingActivityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, ownerUserId, "active")).thenReturn(Optional.of(ownerMember));
        when(materialRepository.findById(otherMatId)).thenReturn(Optional.of(otherFarmMaterial));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> materialUsageService.createMaterialUsage(ownerUserId, request));

        assertTrue(exception.getMessage().contains("không thuộc trang trại"));
    }

    @Test
    @DisplayName("Tạo MaterialUsage thất bại nếu Material inactive")
    void createMaterialUsage_MaterialInactive_ThrowsBadRequest() {
        CreateMaterialUsageReq request = new CreateMaterialUsageReq(
                activityId, inactiveMaterialId, null, LocalDateTime.now(), new BigDecimal("1.000"), "cái", null, null, null, null
        );

        when(farmingActivityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, ownerUserId, "active")).thenReturn(Optional.of(ownerMember));
        when(materialRepository.findById(inactiveMaterialId)).thenReturn(Optional.of(inactiveMaterial));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> materialUsageService.createMaterialUsage(ownerUserId, request));

        assertTrue(exception.getMessage().contains("vô hiệu hóa"));
    }

    @Test
    @DisplayName("Tạo MaterialUsage thất bại nếu Quantity <= 0")
    void createMaterialUsage_QuantityInvalid_ThrowsBadRequest() {
        CreateMaterialUsageReq request = new CreateMaterialUsageReq(
                activityId, fertilizerId, null, LocalDateTime.now(), new BigDecimal("0.000"), "kg", null, null, null, null
        );

        when(farmingActivityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, ownerUserId, "active")).thenReturn(Optional.of(ownerMember));
        when(materialRepository.findById(fertilizerId)).thenReturn(Optional.of(fertilizerMaterial));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> materialUsageService.createMaterialUsage(ownerUserId, request));

        assertTrue(exception.getMessage().contains("phải lớn hơn 0"));
    }

    @Test
    @DisplayName("Tạo MaterialUsage thất bại khi CropSeason đã COMPLETED")
    void createMaterialUsage_ClosedSeason_ThrowsBadRequest() {
        FarmingActivity closedActivity = new FarmingActivity();
        closedActivity.setId(java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 100000));
        closedActivity.setFarm(farm);
        closedActivity.setCropSeason(completedSeason);

        CreateMaterialUsageReq request = new CreateMaterialUsageReq(
                closedActivity.getId(), fertilizerId, null, LocalDateTime.now(), new BigDecimal("1.000"), "kg", null, null, null, null
        );

        when(farmingActivityRepository.findById(closedActivity.getId())).thenReturn(Optional.of(closedActivity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, ownerUserId, "active")).thenReturn(Optional.of(ownerMember));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> materialUsageService.createMaterialUsage(ownerUserId, request));

        assertTrue(exception.getMessage().contains("mùa vụ đã đóng"));
    }

    @Test
    @DisplayName("Xem danh sách MaterialUsage theo Activity có phân trang thành công")
    void getMaterialUsagesByActivity_Success() {
        MaterialUsage usage = new MaterialUsage();
        usage.setId(usageId);
        usage.setFarm(farm);
        usage.setFarmingActivity(activity);
        usage.setMaterial(fertilizerMaterial);
        usage.setUsedBy(ownerMember);

        Pageable pageable = PageRequest.of(0, 20);

        when(farmingActivityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, ownerUserId, "active")).thenReturn(Optional.of(ownerMember));
        when(materialUsageRepository.findByFarmingActivityId(activityId, pageable)).thenReturn(new PageImpl<>(List.of(usage)));

        PageResp<MaterialUsageResp> result = materialUsageService.getMaterialUsagesByActivity(ownerUserId, activityId, pageable);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals(usageId, result.content().get(0).materialUsageId());
    }
}
