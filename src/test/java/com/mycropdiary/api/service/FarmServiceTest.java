package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.farm.*;
import com.mycropdiary.api.entity.AppUser;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.ProductionArea;
import com.mycropdiary.api.repository.AppUserRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.ProductionAreaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class FarmServiceTest {

    @Autowired
    private FarmService farmService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private ProductionAreaRepository productionAreaRepository;

    private AppUser ownerUser;
    private AppUser staffUser;

    @BeforeEach
    void setUp() {
        ownerUser = appUserRepository.save(new AppUser("owner@example.com", "hash", "Farm Owner User", "0901234567", "USER", "ACTIVE"));
        staffUser = appUserRepository.save(new AppUser("staff@example.com", "hash", "Farm Staff User", "0907654321", "USER", "ACTIVE"));
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

    @Test
    void testAssignStaffToProductionArea() {
        CreateFarmRequest farmReq = new CreateFarmRequest(
                "FARM003",
                "Delta Rice Farm",
                "789 River Rd",
                "An Giang",
                "Long Xuyen",
                "My Phu",
                null, null, new BigDecimal("20000.00")
        );
        FarmResponse farmRes = farmService.createFarm(ownerUser.getId(), farmReq);

        AddFarmMemberRequest addMemberReq = new AddFarmMemberRequest("staff@example.com", "STAFF", "Area Worker");
        FarmMemberResponse staffMember = farmService.addMember(ownerUser.getId(), farmRes.id(), addMemberReq);

        Farm farmEntity = farmRepository.findById(farmRes.id()).orElseThrow();
        ProductionArea area = productionAreaRepository.save(new ProductionArea(farmEntity, "AREA01", "Zone A Rice Field", new BigDecimal("5000.00"), "Rice field plot", "ACTIVE"));

        AssignStaffAreaRequest assignReq = new AssignStaffAreaRequest(area.getId(), null, null);
        StaffAreaAssignmentResponse assignment = farmService.assignStaffToArea(ownerUser.getId(), farmRes.id(), staffMember.id(), assignReq);

        assertNotNull(assignment.id());
        assertEquals(area.getId(), assignment.productionAreaId());
        assertTrue(assignment.active());
    }
}
