package com.houseleasing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * 房屋租赁系统应用程序测试类
 *
 * @author hongwenhao
 * @description 使用 H2 内存数据库进行 Spring Boot 上下文加载测试，
 *              禁用 RabbitMQ 监听器和 Redis 仓库以简化测试环境
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=LEGACY",      // 使用 H2 内存数据库（兼容旧版语法）
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.rabbitmq.listener.simple.auto-startup=false",        // 禁用 RabbitMQ 消费者，避免连接失败
        "spring.data.redis.repositories.enabled=false"               // 禁用 Redis 仓库，避免连接失败
})
class HouseLeasingApplicationTests {

    /**
     * 测试 Spring 应用上下文能否正常加载
     * 验证所有 Bean 的依赖注入配置是否正确
     */
    @Test
    void contextLoads() {
    }
}
