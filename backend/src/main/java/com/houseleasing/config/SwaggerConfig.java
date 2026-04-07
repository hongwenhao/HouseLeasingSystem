package com.houseleasing.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 文档配置类
 *
 * @author hongwenhao
 * @description 配置 SpringDoc OpenAPI 文档，包括 API 基本信息和 Bearer Token 认证方案，
 *              访问地址：/swagger-ui.html 或 /v3/api-docs
 */
@Configuration
public class SwaggerConfig {

    /**
     * 配置 OpenAPI 文档信息和全局安全方案
     * 添加 Bearer JWT 认证支持，允许在 Swagger UI 中输入 Token 进行接口调试
     *
     * @return OpenAPI 配置对象
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("House Leasing System API")       // API 文档标题
                        .description("Backend API for House Leasing System") // API 描述
                        .version("1.0.0"))                        // API 版本
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")      // 声明 Token 格式为 JWT
                                        .description("Enter JWT token"))); // 提示信息
    }
}
