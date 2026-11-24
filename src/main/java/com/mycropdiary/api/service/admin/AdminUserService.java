package com.mycropdiary.api.service.admin;

import com.mycropdiary.api.dto.admin.UpdateUserStatusReqDTO;
import com.mycropdiary.api.dto.admin.UserRespDTO;
import com.mycropdiary.api.dto.common.PageResp;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface AdminUserService {

    PageResp<UserRespDTO> getUsers(String status, String search, Pageable pageable);

    UserRespDTO getUserById(Integer userId);

    UserRespDTO createUser(Integer adminUserId, com.mycropdiary.api.dto.admin.CreateAdminUserReqDTO request);

    UserRespDTO updateUserStatus(Integer adminUserId, Integer targetUserId, UpdateUserStatusReqDTO request);

    UserRespDTO updateUserProfile(Integer adminUserId, Integer targetUserId, Map<String, Object> request);

    UserRespDTO updateUser(Integer adminUserId, Integer targetUserId, com.mycropdiary.api.dto.admin.UpdateUserReqDTO request);
}
