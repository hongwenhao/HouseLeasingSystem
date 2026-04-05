package com.houseleasing.service.impl;

import com.houseleasing.common.PageResult;
import com.houseleasing.dto.HouseSearchRequest;
import com.houseleasing.entity.House;
import com.houseleasing.mapper.HouseMapper;
import com.houseleasing.service.HouseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

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
class HouseServiceImplSearchFallbackTests {

    @Autowired
    private HouseService houseService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @SpyBean
    private HouseMapper houseMapper;

    @BeforeEach
    void setupSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS houses");
        jdbcTemplate.execute("""
                CREATE TABLE houses (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    title VARCHAR(200),
                    description VARCHAR(1000),
                    address VARCHAR(300),
                    area DECIMAL(10,2),
                    province VARCHAR(50),
                    city VARCHAR(50),
                    district VARCHAR(50),
                    price DECIMAL(10,2),
                    deposit DECIMAL(10,2),
                    house_type VARCHAR(50),
                    owner_type VARCHAR(50),
                    status VARCHAR(20),
                    water_fee DECIMAL(10,2),
                    electric_fee DECIMAL(10,2),
                    gas_fee DECIMAL(10,2),
                    property_fee DECIMAL(10,2),
                    internet_fee DECIMAL(10,2),
                    water_fee_type VARCHAR(20),
                    electric_fee_type VARCHAR(20),
                    gas_fee_type VARCHAR(20),
                    property_fee_type VARCHAR(20),
                    internet_fee_type VARCHAR(20),
                    cover_image VARCHAR(500),
                    tags VARCHAR(500),
                    workflow_instance_id VARCHAR(100),
                    rooms INT,
                    halls INT,
                    bathrooms INT,
                    floor INT,
                    total_floor INT,
                    decoration VARCHAR(20),
                    images VARCHAR(2000),
                    owner_id BIGINT,
                    view_count INT,
                    create_time TIMESTAMP,
                    update_time TIMESTAMP
                )
                """);
    }

    @Test
    void shouldFallbackToIdDescSortWhenComplexQueryFails() {
        // 数据刻意设置为 create_time 与 id 方向相反：
        // id=1 的 create_time 更新，id=2 的 create_time 更早
        // 若仍按 create_time DESC 排序会返回 [1,2]；按 id DESC 应返回 [2,1]
        jdbcTemplate.update("INSERT INTO houses (status, create_time) VALUES ('ONLINE', TIMESTAMP '2026-01-01 00:00:00')");
        jdbcTemplate.update("INSERT INTO houses (status, create_time) VALUES ('ONLINE', TIMESTAMP '2025-01-01 00:00:00')");

        doThrow(new RuntimeException("Out of sort memory")).when(houseMapper).selectByCondition(any(), any());

        HouseSearchRequest request = new HouseSearchRequest();
        request.setPage(1);
        request.setSize(10);

        PageResult<House> result = houseService.searchHouses(request);

        assertEquals(2L, result.getTotal());
        assertEquals(2, result.getRecords().size());
        assertEquals(2L, result.getRecords().get(0).getId());
        assertEquals(1L, result.getRecords().get(1).getId());
    }
}
