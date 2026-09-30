package com.mycropdiary.api.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

// [AI_CHANGE] Root cause: Thay thế HTTP Basic authentication bằng JWT Bearer Token
// [AI_CHANGE] Mechanism: Đăng ký JwtAuthenticationFilter trước UsernamePasswordAuthenticationFilter,
//             bỏ httpBasic(), cho phép /api/v1/auth/**, /h2-console/**, Swagger và Knowledge public
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/api/v1/auth/**",
                                "/h2-console/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/knowledge/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/modules").permitAll()
                        // [AI_CHANGE] Root cause: Admin API phải được bảo vệ tường minh ở HTTP filter level (defense-in-depth)
                        // [AI_CHANGE] Mechanism: Chặn mọi request tới /api/v1/admin/** nếu không có ROLE_ADMIN,
                        //             kết hợp với @PreAuthorize ở Controller tạo bảo vệ 2 lớp
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                // [AI_CHANGE] Root cause: Tránh trả về 403 mặc định rỗng hoặc HTML khi không có token / sai quyền
                // [AI_CHANGE] Mechanism: Custom EntryPoint trả 401 Unauthorized và AccessDeniedHandler trả 403 Forbidden với ApiResponse JSON chuẩn
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            String body = "{\"success\":false,\"message\":\"Yêu cầu chưa được xác thực. Vui lòng cung cấp JWT token hợp lệ.\",\"data\":null,\"timestamp\":\"" + Instant.now() + "\"}";
                            response.getWriter().write(body);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            String body = "{\"success\":false,\"message\":\"Bạn không có quyền truy cập tài nguyên này (yêu cầu vai trò ADMIN).\",\"data\":null,\"timestamp\":\"" + Instant.now() + "\"}";
                            response.getWriter().write(body);
                        })
                )
                // [AI_CHANGE] Cho phép iframe H2 console trong local dev
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                // [AI_CHANGE] Đăng ký JWT filter thay vì httpBasic
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // [AI_CHANGE] Expose AuthenticationManager bean cho AuthService dùng khi đăng nhập
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
