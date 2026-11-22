package com.mycropdiary.api.service.admin;

import com.mycropdiary.api.dto.admin.UpdateUserStatusReq;
import com.mycropdiary.api.dto.admin.UserResp;
import com.mycropdiary.api.dto.common.PageResp;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface AdminUserService {

    PageResp<UserResp> getUsers(String status, String search, Pageable pageable);

    UserResp getUserById(Integer userId);

    UserResp createUser(Integer adminUserId, com.mycropdiary.api.dto.admin.CreateAdminUserReq request);

    UserResp updateUserStatus(Integer adminUserId, Integer targetUserId, UpdateUserStatusReq request);

    UserResp updateUserProfile(Integer adminUserId, Integer targetUserId, Map<String, Object> request);

    UserResp updateUser(Integer adminUserId, Integer targetUserId, com.mycropdiary.api.dto.admin.UpdateAdminUserReq request);
}
