package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.auth.*;
import com.mycropdiary.api.dto.common.ApiResponse;
import com.mycropdiary.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// [AI_CHANGE] Root cause: AuthController cần tích hợp đầy đủ với AuthService cho UC-03 và UC-04
// [AI_CHANGE] Mechanism: Mỗi endpoint gọi service tương ứng, trả về ApiResponse chuẩn hóa
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * UC-04: Đăng ký tài khoản mới.
     * Tạo user PENDING, sinh OTP 6 số và gửi email xác thực.
     */
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
    @PostMapping("/verify-otp")
    public ApiResponse<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = authService.verifyOtp(request);
        return ApiResponse.ok("Xác thực OTP thành công. Tài khoản đã được kích hoạt.", response);
    }

    /**
     * UC-03: Đăng nhập bằng email và mật khẩu.
     * Trả về access token + refresh token.
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ApiResponse.ok("Đăng nhập thành công.", response);
    }

    /**
     * Gửi lại OTP nếu email chưa nhận được hoặc OTP đã hết hạn.
     */
    @PostMapping("/resend-otp")
    public ApiResponse<Map<String, String>> resendOtp(@RequestParam String email) {
        authService.resendOtp(email);
        return ApiResponse.ok(
                "Mã OTP mới đã được gửi tới email của bạn.",
                Map.of("email", email)
        );
    }
}
