package com.mycropdiary.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// [AI_CHANGE] Root cause: Cần tài liệu hóa REST API và cung cấp giao diện Swagger UI để test API trực quan
// [AI_CHANGE] Mechanism: Cấu hình OpenAPI 3.0 với JWT Bearer Authentication scheme (Authorize button)
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuthentication";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MyCropDiary Backend REST API")
                        .description("Hệ thống quản lý chi phí và nhật ký canh tác theo tiêu chuẩn VietGAP kết hợp trợ lý AI (SEP490_05 - Đại học FPT Cần Thơ)")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("MyCropDiary Dev Team")
                                .email("support@mycropdiary.vn"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Nhập JWT Access Token nhận được từ API /api/v1/auth/login hoặc /api/v1/auth/verify-otp (không cần gõ tiền tố 'Bearer ')")));
    }
}
