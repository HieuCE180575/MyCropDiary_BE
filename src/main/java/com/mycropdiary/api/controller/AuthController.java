package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.auth.*;
import com.mycropdiary.api.dto.common.ApiResponse;
import com.mycropdiary.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// [AI_CHANGE] Root cause: AuthController cần tích hợp đầy đủ với AuthService cho UC-03 và UC-04
// [AI_CHANGE] Mechanism: Mỗi endpoint gọi service tương ứng, trả về ApiResponse chuẩn hóa; thêm OpenAPI annotations
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "01. Xác thực & Tài khoản (Auth)", description = "APIs đăng ký, xác thực OTP email, đăng nhập và cấp JWT Bearer tokens")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * UC-04: Đăng ký tài khoản mới.
     * Tạo user PENDING, sinh OTP 6 số và gửi email xác thực.
     */
    @Operation(summary = "Đăng ký tài khoản mới (UC-04)", description = "Tạo user mới ở trạng thái PENDING, sinh mã OTP 6 số gửi qua email để kích hoạt.")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.ok(
                "Đăng ký thành công. Vui lòng kiểm tra email để nhận mã OTP xác thực.",
                Map.of("email", request.email())
        );
    }

    /**
     * UC-04: Xác thực OTP qua email.
     * Kích hoạt tài khoản và trả về JWT tokens.
     */
    @Operation(summary = "Xác thực mã OTP (UC-04)", description = "Xác thực mã OTP 6 số nhận từ email. Nếu chính xác, tài khoản được kích hoạt (ACTIVE) và trả về Access Token + Refresh Token.")
    @PostMapping("/verify-otp")
    public ApiResponse<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = authService.verifyOtp(request);
        return ApiResponse.ok("Xác thực OTP thành công. Tài khoản đã được kích hoạt.", response);
    }

    /**
     * UC-03: Đăng nhập bằng email và mật khẩu.
     * Trả về access token + refresh token.
     */
    @Operation(summary = "Đăng nhập hệ thống (UC-03)", description = "Đăng nhập bằng Email và Password, trả về Access Token (15 phút) và Refresh Token (30 ngày).")
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ApiResponse.ok("Đăng nhập thành công.", response);
    }

    /**
     * Gửi lại OTP nếu email chưa nhận được hoặc OTP đã hết hạn.
     */
    @Operation(summary = "Gửi lại mã OTP qua email", description = "Tạo mã OTP mới và gửi lại vào email người dùng nếu mã cũ bị hết hạn hoặc thất lạc.")
    @PostMapping("/resend-otp")
    public ApiResponse<Map<String, String>> resendOtp(@RequestParam String email) {
        authService.resendOtp(email);
        return ApiResponse.ok(
                "Mã OTP mới đã được gửi tới email của bạn.",
                Map.of("email", email)
        );
    }

    // ==================== UC-05: Đăng xuất & Quản lý phiên ====================

    /**
     * UC-05: Đăng xuất – Thu hồi Refresh Token.
     * Vô hiệu hóa refresh token cụ thể, đảm bảo không thể dùng để cấp access token mới.
     */
    @Operation(summary = "Đăng xuất (UC-05)",
            description = "Thu hồi Refresh Token hiện tại. Sau khi đăng xuất, token này không thể dùng để làm mới phiên.")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.ok("Đăng xuất thành công. Phiên đã được đóng.", null);
    }

    /**
     * UC-05: Làm mới Access Token bằng Refresh Token.
     * Kiểm tra Refresh Token hợp lệ (chữ ký JWT + chưa revoked trong DB), cấp Access Token mới.
     */
    @Operation(summary = "Làm mới Access Token (UC-05)",
            description = "Dùng Refresh Token còn hiệu lực để cấp Access Token mới mà không cần đăng nhập lại.")
    @PostMapping("/refresh-token")
    public ApiResponse<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.refreshToken());
        return ApiResponse.ok("Đã cấp Access Token mới.", response);
    }

    // ==================== UC-06: Quên mật khẩu ====================

    /**
     * UC-06: Quên mật khẩu – Gửi OTP đặt lại mật khẩu qua email.
     * Luôn trả về thành công để tránh lộ thông tin email có tồn tại trong hệ thống hay không.
     */
    @Operation(summary = "Quên mật khẩu – Gửi OTP (UC-06)",
            description = "Gửi mã OTP đặt lại mật khẩu tới email người dùng. " +
                          "API luôn trả về thành công để bảo vệ quyền riêng tư email.")
    @PostMapping("/forgot-password")
    public ApiResponse<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ApiResponse.ok(
                "Nếu email tồn tại trong hệ thống, mã OTP đặt lại mật khẩu đã được gửi.",
                Map.of("email", request.email())
        );
    }

    /**
     * UC-06: Đặt lại mật khẩu – Xác minh OTP và cập nhật mật khẩu mới.
     * Sau khi đặt lại thành công, tất cả refresh token cũ sẽ bị thu hồi (buộc đăng nhập lại).
     */
    @Operation(summary = "Đặt lại mật khẩu (UC-06)",
            description = "Xác minh mã OTP nhận từ email và đặt mật khẩu mới. " +
                          "Tất cả phiên đăng nhập cũ sẽ bị đóng sau khi đổi mật khẩu.")
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok("Đặt lại mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới.", null);
    }
}
