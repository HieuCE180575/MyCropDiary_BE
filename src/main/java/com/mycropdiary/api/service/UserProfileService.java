package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.user.ChangePasswordReq;
import com.mycropdiary.api.dto.user.UpdateProfileReq;
import com.mycropdiary.api.dto.user.UserProfileResp;

public interface UserProfileService {
    UserProfileResp getProfile(Integer userId);
    UserProfileResp updateProfile(Integer userId, UpdateProfileReq request);
    void changePassword(Integer userId, ChangePasswordReq request);
}
