package com.houseleasing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 登录接口用例：支持使用手机号或用户名登录
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=LEGACY",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.data.redis.repositories.enabled=false"
})
class AuthControllerLoginTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setupSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS users");
        jdbcTemplate.execute("""
                CREATE TABLE users (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) NOT NULL UNIQUE,
                    phone VARCHAR(20) UNIQUE,
                    email VARCHAR(100) NOT NULL UNIQUE,
                    password VARCHAR(255) NOT NULL,
                    role VARCHAR(20),
                    real_name VARCHAR(50),
                    id_card VARCHAR(18),
                    avatar VARCHAR(500),
                    credit_score INT,
                    is_real_name_auth BOOLEAN,
                    gender INT,
                    status VARCHAR(20),
                    create_time TIMESTAMP,
                    update_time TIMESTAMP
                )
                """);
    }

    @Test
    void shouldLoginWithPhoneNumber() throws Exception {
        // 先通过注册接口写入测试用户（包含用户名与手机号），确保密码编码逻辑与生产一致
        Map<String, Object> registerPayload = new HashMap<>();
        registerPayload.put("username", "user1");
        registerPayload.put("phone", "13800000001");
        registerPayload.put("email", "test@example.com");
        registerPayload.put("password", "password1");
        registerPayload.put("role", "TENANT");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload)))
                .andExpect(status().isOk());

        // 使用手机号作为登录凭据，验证后端能识别并返回 token 与用户信息
        Map<String, Object> loginPayload = new HashMap<>();
        loginPayload.put("username", "13800000001"); // 前端字段名仍为 username，但值为手机号
        loginPayload.put("password", "password1");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", not("")))
                .andExpect(jsonPath("$.data.user.username").value("user1"));
    }

    @Test
    void shouldLoginWithUsername() throws Exception {
        Map<String, Object> registerPayload = new HashMap<>();
        registerPayload.put("username", "user2");
        registerPayload.put("phone", "13800000002");
        registerPayload.put("email", "user2@example.com");
        registerPayload.put("password", "password2");
        registerPayload.put("role", "TENANT");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload)))
                .andExpect(status().isOk());

        Map<String, Object> loginPayload = new HashMap<>();
        loginPayload.put("username", "user2");
        loginPayload.put("password", "password2");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", not("")))
                .andExpect(jsonPath("$.data.user.phone").value("13800000002"));
    }

    @Test
    void shouldRejectLoginWhenPhoneNotFound() throws Exception {
        Map<String, Object> registerPayload = new HashMap<>();
        registerPayload.put("username", "user3");
        registerPayload.put("phone", "13800000003");
        registerPayload.put("email", "user3@example.com");
        registerPayload.put("password", "password3");
        registerPayload.put("role", "TENANT");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload)))
                .andExpect(status().isOk());

        Map<String, Object> loginPayload = new HashMap<>();
        loginPayload.put("username", "13900000000"); // 未注册的手机号
        loginPayload.put("password", "password3");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("用户不存在")));
    }
}
