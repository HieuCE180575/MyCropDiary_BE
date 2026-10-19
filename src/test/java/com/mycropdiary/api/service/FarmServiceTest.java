package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.farm.*;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.exception.BusinessRuleException;
import com.mycropdiary.api.repository.UserRepository;
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
    private UserRepository userRepository;

    private User ownerUser;
    private User staffUser;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        ownerUser = userRepository.save(new User("owner@example.com", "hash", "Farm Owner User", "0901234567", "user", "active"));
        staffUser = userRepository.save(new User("staff@example.com", "hash", "Farm Staff User", "0907654321", "user", "active"));
        anotherUser = userRepository.save(new User("another@example.com", "hash", "Another User", "0909999999", "user", "active"));
    }

    @Test
    void testCreateFarm_OwnerRoleAssignedAndGetById() {
        CreateFarmReq request = new CreateFarmReq(
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

        FarmResp created = farmService.createFarm(ownerUser.getId(), request);
        assertNotNull(created.id());
        assertNotNull(created.farmCode());
        assertEquals("Green Valley Farm", created.farmName());
        assertEquals("OWNER", created.currentUserRole().toUpperCase());

        FarmResp fetched = farmService.getFarmById(ownerUser.getId(), created.id());
        assertEquals("Green Valley Farm", fetched.farmName());
        assertEquals("OWNER", fetched.currentUserRole().toUpperCase());
    }

    @Test
    void testBR_CoordinatesValidation() {
        // Invalid: only latitude provided
        CreateFarmReq invalidReq1 = new CreateFarmReq(
                "FARM_INVALID1", "Test Farm", "Address", "Province", "District", "Ward",
                new BigDecimal("10.5"), null, new BigDecimal("1000.00")
        );
        assertThrows(IllegalArgumentException.class, () -> farmService.createFarm(ownerUser.getId(), invalidReq1));

        // Invalid: latitude out of range
        CreateFarmReq invalidReq2 = new CreateFarmReq(
                "FARM_INVALID2", "Test Farm", "Address", "Province", "District", "Ward",
                new BigDecimal("95.0"), new BigDecimal("105.0"), new BigDecimal("1000.00")
        );
        assertThrows(IllegalArgumentException.class, () -> farmService.createFarm(ownerUser.getId(), invalidReq2));
    }

    @Test
    void testBR_SingleActiveOwnerAndSoleOwnerProtection() {
        CreateFarmReq farmReq = new CreateFarmReq(
                "FARM_OWNER_BR", "Single Owner Farm", "Address", "Province", "District", "Ward",
                null, null, new BigDecimal("5000.00")
        );
        FarmResp farm = farmService.createFarm(ownerUser.getId(), farmReq);

        // Cannot add a 2nd OWNER directly
        AddFarmMemberReq addOwnerReq = new AddFarmMemberReq("another@example.com", "OWNER", "Co-Owner");
        assertThrows(BusinessRuleException.class, () -> farmService.addMember(ownerUser.getId(), farm.id(), addOwnerReq));

        // Cannot deactivate sole active OWNER
        List<FarmMemberResp> members = farmService.getFarmMembers(ownerUser.getId(), farm.id());
        FarmMemberResp ownerMember = members.stream().filter(m -> "OWNER".equalsIgnoreCase(m.farmRole())).findFirst().orElseThrow();

        assertThrows(BusinessRuleException.class, () -> farmService.removeMember(ownerUser.getId(), farm.id(), ownerMember.id()));
    }

    @Test
    void testBR_ProductionAreaSizeAndStaffAccessScope() {
        CreateFarmReq farmReq = new CreateFarmReq(
                "FARM_AREA_BR", "Area Delta Farm", "Address", "Province", "District", "Ward",
                null, null, new BigDecimal("10000.00")
        );
        FarmResp farm = farmService.createFarm(ownerUser.getId(), farmReq);

        // Create Production Area 1 (Zone A)
        CreateZoneReq area1Req = new CreateZoneReq("Zone A", new BigDecimal("6000.00"), "Zone A notes");
        ZoneResp area1 = farmService.createZone(ownerUser.getId(), farm.id(), area1Req);
        assertEquals("Zone A", area1.zoneName());

        // Create Zone with duplicate name in same farm -> Throws IllegalArgumentException
        CreateZoneReq duplicateReq = new CreateZoneReq("Zone A", new BigDecimal("5000.00"), "Duplicate Zone A");
        assertThrows(IllegalArgumentException.class, () -> farmService.createZone(ownerUser.getId(), farm.id(), duplicateReq));

        // Create Production Area 2 (Zone B)
        CreateZoneReq area2Req = new CreateZoneReq("Zone B", new BigDecimal("5000.00"), "Zone B notes");
        farmService.createZone(ownerUser.getId(), farm.id(), area2Req);

        // Add staff member and assign to Area 1 only
        AddFarmMemberReq addStaffReq = new AddFarmMemberReq("staff@example.com", "STAFF", "Worker");
        FarmMemberResp staffMember = farmService.addMember(ownerUser.getId(), farm.id(), addStaffReq);

        AssignStaffZoneReq assignReq = new AssignStaffZoneReq(area1.id());
        farmService.assignStaffToZone(ownerUser.getId(), farm.id(), staffMember.id(), assignReq);

        // Staff views zones -> receives only Zone 1
        List<ZoneResp> staffAreas = farmService.getZones(staffUser.getId(), farm.id());
        assertEquals(1, staffAreas.size());
        assertEquals("Zone A", staffAreas.get(0).zoneName());
    }

    @Test
    void testAddMember_AndStaffAccessControl() {
        CreateFarmReq farmReq = new CreateFarmReq(
                "FARM002",
                "Organic Farm",
                "456 Eco St",
                "Can Tho",
                "Ninh Kieu",
                "An Hoa",
                null, null, new BigDecimal("10000.00")
        );
        FarmResp farm = farmService.createFarm(ownerUser.getId(), farmReq);

        // Add staff member
        AddFarmMemberReq addMemberReq = new AddFarmMemberReq("staff@example.com", "STAFF", "Field Technician");
        FarmMemberResp addedMember = farmService.addMember(ownerUser.getId(), farm.id(), addMemberReq);

        assertEquals("STAFF", addedMember.farmRole().toUpperCase());
        assertEquals("Field Technician", addedMember.jobTitle());

        // Staff can view farm details
        FarmResp staffView = farmService.getFarmById(staffUser.getId(), farm.id());
        assertEquals("STAFF", staffView.currentUserRole().toUpperCase());

        // Staff cannot update farm details (Permission check)
        UpdateFarmReq updateReq = new UpdateFarmReq(
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
