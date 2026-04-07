package com.houseleasing.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 *
 * @author hongwenhao
 * @description 配置 MyBatis-Plus 插件，包括分页插件（针对 MySQL），
 *              并扫描 Mapper 接口所在的包路径
 */
@Configuration
@MapperScan("com.houseleasing.mapper") // 扫描所有 Mapper 接口并注册为 Spring Bean
public class MyBatisPlusConfig {

    /**
     * 配置 MyBatis-Plus 拦截器，添加 MySQL 数据库的分页插件
     * 分页插件用于自动处理 SELECT 查询的分页逻辑（自动添加 LIMIT 子句）
     *
     * @return MyBatis-Plus 拦截器实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加针对 MySQL 的分页内部拦截器
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
