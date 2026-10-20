package com.mycropdiary.api.service.admin;

import com.mycropdiary.api.dto.admin.UpdateUserStatusReq;
import com.mycropdiary.api.dto.admin.UserResp;
import com.mycropdiary.api.dto.common.PageResp;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    PageResp<UserResp> getUsers(String status, String search, Pageable pageable);

    UserResp getUserById(Integer userId);

    UserResp updateUserStatus(Integer adminUserId, Integer targetUserId, UpdateUserStatusReq request);
}
