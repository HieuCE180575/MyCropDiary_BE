package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.auth.*;

// [AI_CHANGE] Root cause: Cần service interface cho module Authentication (UC-03, UC-04, UC-05, UC-06)
public interface AuthService {

    /** UC-04: Đăng ký tài khoản mới, lưu trạng thái PENDING, tạo OTP và gửi email xác thực */
    void register(RegisterAccReqDTO request);

    /** UC-04: Xác thực OTP email, chuyển tài khoản sang ACTIVE, trả về JWT tokens */
    AuthRespDTO verifyOtp(VerifyOtpReqDTO request);

    /** UC-03: Đăng nhập bằng email + password, trả về JWT access + refresh token */
    AuthRespDTO login(LoginReqDTO request);

    /** Gửi lại OTP nếu người dùng chưa nhận được email */
    void resendOtp(String email);

    // [AI_CHANGE] UC-05: Thu hồi Refresh Token đang hoạt động (đăng xuất)
    /** UC-05: Đăng xuất – vô hiệu hóa Refresh Token cụ thể khỏi DB */
    void logout(String refreshToken);

    // [AI_CHANGE] UC-05: Cấp mới Access Token từ Refresh Token hợp lệ
    /** UC-05: Làm mới Access Token bằng Refresh Token còn hiệu lực */
    AuthRespDTO refreshToken(String refreshToken);

    // [AI_CHANGE] UC-06: Gửi OTP đặt lại mật khẩu qua email
    /** UC-06: Quên mật khẩu – gửi OTP đặt lại qua email */
    void forgotPassword(String email);

    // [AI_CHANGE] UC-06: Xác minh OTP và đặt lại mật khẩu mới
    /** UC-06: Đặt lại mật khẩu sau khi xác minh OTP */
    void resetPassword(ResetPasswordReqDTO request);

    /** Đăng nhập / Đăng ký tài khoản nhanh qua Google OAuth2 / ID Token */
    AuthRespDTO loginWithGoogle(GoogleAuthReqDTO request);
}
