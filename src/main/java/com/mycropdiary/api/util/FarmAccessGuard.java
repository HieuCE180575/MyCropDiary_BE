package com.mycropdiary.api.util;

import org.springframework.stereotype.Component;

@Component
public class FarmAccessGuard {
    public void requireFarmAccess(Long userId, Long farmId) {
        // TODO: validate active FarmMember and role before every farm-scoped operation.
    }

    public void requireProductionAreaAccess(Long userId, Long productionAreaId) {
        // TODO: validate StaffAreaAssignment for FARM_STAFF.
    }
}
