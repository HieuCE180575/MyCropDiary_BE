package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.assignment.AssignedZoneResp;
import com.mycropdiary.api.dto.farm.CreateZoneReq;
import com.mycropdiary.api.dto.farm.UpdateZoneReq;
import com.mycropdiary.api.dto.farm.ZoneDetailResp;
import com.mycropdiary.api.dto.farm.ZoneResp;

import java.util.List;

/**
 * Service quản lý Phân khu / Vùng sản xuất (Zone) - UC-16: Manage My Zones.
 */
public interface ZoneService {
    // UC-16: Tạo Zone mới
    ZoneResp createZone(Integer currentUserId, Integer farmId, CreateZoneReq request);

    // UC-16: Cập nhật Zone
    ZoneResp updateZone(Integer currentUserId, Integer zoneId, UpdateZoneReq request);

    // UC-16: Xóa Zone
    void deleteZone(Integer currentUserId, Integer zoneId);

    // UC-16.6: Xem chi tiết Zone
    ZoneDetailResp getZoneById(Integer currentUserId, Integer zoneId);

    // UC-16.3, 16.4, 16.5: Xem, Tìm kiếm và Lọc danh sách Zones theo Farm
    List<ZoneResp> searchZones(Integer currentUserId, Integer farmId, String keyword, String status);

    // UC-16.1: Xem danh sách Zone được phân công cho nhân viên hiện tại
    List<AssignedZoneResp> getAssignedZones(Integer currentUserId, Integer farmId);

    // UC-16.2: Xem thông tin chi tiết Zone được phân công
    ZoneDetailResp getAssignedZoneDetail(Integer currentUserId, Integer zoneId);
}
