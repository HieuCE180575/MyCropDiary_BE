package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.assignment.AssignedZoneRespDTO;
import com.mycropdiary.api.dto.farm.CreateZoneReqDTO;
import com.mycropdiary.api.dto.farm.UpdateZoneReqDTO;
import com.mycropdiary.api.dto.farm.ZoneDetailRespDTO;
import com.mycropdiary.api.dto.farm.ZoneRespDTO;

import java.util.List;

/**
 * Service quản lý Phân khu / Vùng sản xuất (Zone) - UC-16: Manage My Zones.
 */
public interface ZoneService {
    // UC-16: Tạo Zone mới
    ZoneRespDTO createZone(Integer currentUserId, Integer farmId, CreateZoneReqDTO request);

    // UC-16: Cập nhật Zone
    ZoneRespDTO updateZone(Integer currentUserId, Integer zoneId, UpdateZoneReqDTO request);

    // UC-16: Xóa Zone
    void deleteZone(Integer currentUserId, Integer zoneId);

    // UC-16.6: Xem chi tiết Zone
    ZoneDetailRespDTO getZoneById(Integer currentUserId, Integer zoneId);

    // UC-16.3, 16.4, 16.5: Xem, Tìm kiếm và Lọc danh sách Zones theo Farm
    List<ZoneRespDTO> searchZones(Integer currentUserId, Integer farmId, String keyword, String status);

    // UC-16.1: Xem danh sách Zone được phân công cho nhân viên hiện tại
    List<AssignedZoneRespDTO> getAssignedZones(Integer currentUserId, Integer farmId);

    // UC-16.2: Xem thông tin chi tiết Zone được phân công
    ZoneDetailRespDTO getAssignedZoneDetail(Integer currentUserId, Integer zoneId);
}
