package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.user.ChangePasswordReqDTO;
import com.mycropdiary.api.dto.user.UpdateProfileReqDTO;
import com.mycropdiary.api.dto.user.UserProfileRespDTO;

public interface UserProfileService {
    UserProfileRespDTO getProfile(Integer userId);
    UserProfileRespDTO updateProfile(Integer userId, UpdateProfileReqDTO request);
    void changePassword(Integer userId, ChangePasswordReqDTO request);
}
