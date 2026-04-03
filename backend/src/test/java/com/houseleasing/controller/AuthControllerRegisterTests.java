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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 注册接口用例：验证邮箱必填且唯一
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
class AuthControllerRegisterTests {

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
                    last_credit_add_date DATE,
                    create_time TIMESTAMP,
                    update_time TIMESTAMP
                )
                """);
    }

    @Test
    void shouldRejectRegistrationWhenEmailMissing() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", "user1");
        payload.put("phone", "13800000001");
        payload.put("password", "password1");
        payload.put("role", "TENANT");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("邮箱不能为空")));
    }

    @Test
    void shouldRejectDuplicateEmailRegistration() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", "user1");
        payload.put("phone", "13800000001");
        payload.put("email", "test@example.com");
        payload.put("password", "password1");
        payload.put("role", "TENANT");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        payload.put("username", "user2");
        payload.put("phone", "13800000002");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("邮箱已被注册")));
    }
}
