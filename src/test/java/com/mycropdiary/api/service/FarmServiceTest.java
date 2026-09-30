package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.farm.*;
import com.mycropdiary.api.entity.AppUser;
import com.mycropdiary.api.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class FarmServiceTest {

    @Autowired
    private FarmService farmService;

    @Autowired
    private AppUserRepository appUserRepository;

    private AppUser ownerUser;
    private AppUser staffUser;
    private AppUser anotherUser;

    @BeforeEach
    void setUp() {
        ownerUser = appUserRepository.save(new AppUser("owner@example.com", "hash", "Farm Owner User", "0901234567", "USER", "ACTIVE"));
        staffUser = appUserRepository.save(new AppUser("staff@example.com", "hash", "Farm Staff User", "0907654321", "USER", "ACTIVE"));
        anotherUser = appUserRepository.save(new AppUser("another@example.com", "hash", "Another User", "0909999999", "USER", "ACTIVE"));
    }

    @Test
    void testCreateFarm_OwnerRoleAssignedAndGetById() {
        CreateFarmRequest request = new CreateFarmRequest(
                "FARM001",
                "Green Valley Farm",
                "123 Agriculture Way",
                "Lam Dong",
                "Da Lat",
                "Ward 1",
                new BigDecimal("11.9404"),
                new BigDecimal("108.4583"),
                new BigDecimal("5000.00")
        );

        FarmResponse created = farmService.createFarm(ownerUser.getId(), request);
        assertNotNull(created.id());
        assertEquals("FARM001", created.farmCode());
        assertEquals("Green Valley Farm", created.farmName());
        assertEquals("OWNER", created.currentUserRole());

        FarmResponse fetched = farmService.getFarmById(ownerUser.getId(), created.id());
        assertEquals("Green Valley Farm", fetched.farmName());
        assertEquals("OWNER", fetched.currentUserRole());
    }

    @Test
    void testBR_CoordinatesValidation() {
        // Invalid: only latitude provided
        CreateFarmRequest invalidReq1 = new CreateFarmRequest(
                "FARM_INVALID1", "Test Farm", "Address", "Province", "District", "Ward",
                new BigDecimal("10.5"), null, new BigDecimal("1000.00")
        );
        assertThrows(IllegalArgumentException.class, () -> farmService.createFarm(ownerUser.getId(), invalidReq1));

        // Invalid: latitude out of range
        CreateFarmRequest invalidReq2 = new CreateFarmRequest(
                "FARM_INVALID2", "Test Farm", "Address", "Province", "District", "Ward",
                new BigDecimal("95.0"), new BigDecimal("105.0"), new BigDecimal("1000.00")
        );
        assertThrows(IllegalArgumentException.class, () -> farmService.createFarm(ownerUser.getId(), invalidReq2));
    }

    @Test
    void testBR_SingleActiveOwnerAndSoleOwnerProtection() {
        CreateFarmRequest farmReq = new CreateFarmRequest(
                "FARM_OWNER_BR", "Single Owner Farm", "Address", "Province", "District", "Ward",
                null, null, new BigDecimal("5000.00")
        );
        FarmResponse farm = farmService.createFarm(ownerUser.getId(), farmReq);

        // Cannot add a 2nd OWNER directly
        AddFarmMemberRequest addOwnerReq = new AddFarmMemberRequest("another@example.com", "OWNER", "Co-Owner");
        assertThrows(IllegalStateException.class, () -> farmService.addMember(ownerUser.getId(), farm.id(), addOwnerReq));

        // Cannot deactivate sole active OWNER
        List<FarmMemberResponse> members = farmService.getFarmMembers(ownerUser.getId(), farm.id());
        FarmMemberResponse ownerMember = members.stream().filter(m -> "OWNER".equals(m.farmRole())).findFirst().orElseThrow();

        assertThrows(IllegalStateException.class, () -> farmService.removeMember(ownerUser.getId(), farm.id(), ownerMember.id()));
    }

    @Test
    void testBR_ProductionAreaSizeAndStaffAccessScope() {
        CreateFarmRequest farmReq = new CreateFarmRequest(
                "FARM_AREA_BR", "Area Delta Farm", "Address", "Province", "District", "Ward",
                null, null, new BigDecimal("10000.00")
        );
        FarmResponse farm = farmService.createFarm(ownerUser.getId(), farmReq);

        // Create Production Area 1 (6000 m2)
        CreateProductionAreaRequest area1Req = new CreateProductionAreaRequest("AREA01", "Zone A", new BigDecimal("6000.00"), "Zone A notes");
        ProductionAreaResponse area1 = farmService.createProductionArea(ownerUser.getId(), farm.id(), area1Req);
        assertEquals("AREA01", area1.areaCode());

        // Create Production Area 2 (5000 m2) -> Total 11000 m2 > 10000 m2 -> Throws IllegalArgumentException
        CreateProductionAreaRequest area2Req = new CreateProductionAreaRequest("AREA02", "Zone B", new BigDecimal("5000.00"), "Zone B notes");
        assertThrows(IllegalArgumentException.class, () -> farmService.createProductionArea(ownerUser.getId(), farm.id(), area2Req));

        // Add staff member and assign to Area 1
        AddFarmMemberRequest addStaffReq = new AddFarmMemberRequest("staff@example.com", "STAFF", "Worker");
        FarmMemberResponse staffMember = farmService.addMember(ownerUser.getId(), farm.id(), addStaffReq);

        AssignStaffAreaRequest assignReq = new AssignStaffAreaRequest(area1.id(), null, null);
        farmService.assignStaffToArea(ownerUser.getId(), farm.id(), staffMember.id(), assignReq);

        // Staff views production areas -> receives only Area 1
        List<ProductionAreaResponse> staffAreas = farmService.getProductionAreas(staffUser.getId(), farm.id());
        assertEquals(1, staffAreas.size());
        assertEquals("AREA01", staffAreas.get(0).areaCode());
    }

    @Test
    void testAddMember_AndStaffAccessControl() {
        CreateFarmRequest farmReq = new CreateFarmRequest(
                "FARM002",
                "Organic Farm",
                "456 Eco St",
                "Can Tho",
                "Ninh Kieu",
                "An Hoa",
                null, null, new BigDecimal("10000.00")
        );
        FarmResponse farm = farmService.createFarm(ownerUser.getId(), farmReq);

        // Add staff member
        AddFarmMemberRequest addMemberReq = new AddFarmMemberRequest("staff@example.com", "STAFF", "Field Technician");
        FarmMemberResponse addedMember = farmService.addMember(ownerUser.getId(), farm.id(), addMemberReq);

        assertEquals("STAFF", addedMember.farmRole());
        assertEquals("Field Technician", addedMember.jobTitle());

        // Staff can view farm details
        FarmResponse staffView = farmService.getFarmById(staffUser.getId(), farm.id());
        assertEquals("STAFF", staffView.currentUserRole());

        // Staff cannot update farm details (Permission check)
        UpdateFarmRequest updateReq = new UpdateFarmRequest(
                "Unauthorized Update Name",
                "New Address",
                "Can Tho", "Ninh Kieu", "An Hoa",
                null, null, new BigDecimal("12000.00"), "ACTIVE"
        );

        assertThrows(AccessDeniedException.class, () -> {
            farmService.updateFarm(staffUser.getId(), farm.id(), updateReq);
        });
    }
}
