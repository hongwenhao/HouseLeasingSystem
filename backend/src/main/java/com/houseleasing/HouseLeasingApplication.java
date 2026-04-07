package com.houseleasing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 房屋租赁系统主启动类
 *
 * @author hongwenhao
 * @description Spring Boot 应用程序入口，启用缓存和异步处理功能
 */
@SpringBootApplication
@EnableCaching  // 启用 Spring 缓存注解支持
@EnableAsync    // 启用异步方法执行支持
public class HouseLeasingApplication {

    /**
     * 应用程序入口方法
     *
     * @param args 命令行启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(HouseLeasingApplication.class, args);
    }
}
