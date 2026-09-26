package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.auth.AuthResponse;
import com.mycropdiary.api.dto.auth.LoginRequest;
import com.mycropdiary.api.dto.auth.RegisterRequest;
import com.mycropdiary.api.dto.auth.VerifyOtpRequest;

// [AI_CHANGE] Root cause: Cần service interface cho module Authentication (UC-03, UC-04)
public interface AuthService {

    /** UC-04: Đăng ký tài khoản mới, lưu trạng thái PENDING, tạo OTP và gửi email xác thực */
    void register(RegisterRequest request);

    /** UC-04: Xác thực OTP email, chuyển tài khoản sang ACTIVE, trả về JWT tokens */
    AuthResponse verifyOtp(VerifyOtpRequest request);

    /** UC-03: Đăng nhập bằng email + password, trả về JWT access + refresh token */
    AuthResponse login(LoginRequest request);

    /** Gửi lại OTP nếu người dùng chưa nhận được email */
    void resendOtp(String email);
}
