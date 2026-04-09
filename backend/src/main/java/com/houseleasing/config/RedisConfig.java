package com.houseleasing.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 配置类
 *
 * @author hongwenhao
 * @description 配置 Redis 连接模板和缓存管理器，使用 Jackson 序列化，
 *              支持 Java 8 时间类型，默认缓存 TTL 为 30 分钟
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisConfig { // Redis 序列化、缓存管理与异常处理配置

    /**
     * 配置 RedisTemplate，使用 Jackson JSON 序列化存储 Java 对象
     * Key 使用 String 序列化，Value 使用 JSON 序列化
     *
     * @param factory Redis 连接工厂
     * @return 配置好的 RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) { // 配置 RedisTemplate 序列化策略
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // 配置 Jackson ObjectMapper，启用多态类型处理以支持复杂对象的序列化
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY // 将类型信息作为 JSON 属性存储
        );
        objectMapper.registerModule(new JavaTimeModule()); // 支持 Java 8 时间类型（LocalDate 等）

        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        // Key 使用 String 序列化，Value 使用 JSON 序列化
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置 Spring Cache 的 Redis 缓存管理器
     * 缓存 Key 使用 String 序列化，Value 使用 JSON 序列化，默认 TTL 30 分钟
     *
     * @param factory Redis 连接工厂
     * @return 配置好的 CacheManager 实例
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) { // 配置 Spring Cache 的 Redis 管理器
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

        // 配置缓存默认设置：30分钟TTL、String序列化Key、JSON序列化Value、不缓存null值
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // 缓存 30 分钟后自动失效
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues(); // 禁止缓存 null 值，避免缓存穿透

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .build();
    }

    /**
     * 缓存异常处理器：当读取到历史/损坏缓存值导致反序列化失败时，删除该 key 并回源数据库。
     *
     * <p>说明：历史环境可能存在由旧序列化策略写入的值（如 "[]"），与当前 Jackson 多态反序列化不兼容。
     * 这里在 GET 失败时自动清理坏缓存，避免请求直接报错。</p>
     *
     * @return 自定义缓存错误处理器
     */
    @Bean
    public CacheErrorHandler cacheErrorHandler() { // 配置缓存异常兜底处理器
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("缓存读取失败，回源数据库。缓存名={}, 键={}, 异常类型={}, 原因={}",
                        cache != null ? cache.getName() : "unknown", key,
                        exception.getClass().getSimpleName(), exception.getMessage());
                if (cache != null && key != null) {
                    try {
                        cache.evict(key);
                    } catch (Exception evictException) {
                        log.warn("清理损坏缓存项失败。缓存名={}, 键={}, 异常类型={}, 原因={}",
                                cache.getName(), key, evictException.getClass().getSimpleName(),
                                evictException.getMessage());
                    }
                }
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                log.warn("缓存写入失败。缓存名={}, 键={}, 异常类型={}, 原因={}",
                        cache != null ? cache.getName() : "unknown", key,
                        exception.getClass().getSimpleName(), exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.warn("缓存删除失败。缓存名={}, 键={}, 异常类型={}, 原因={}",
                        cache != null ? cache.getName() : "unknown", key,
                        exception.getClass().getSimpleName(), exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                log.warn("缓存清空失败。缓存名={}, 异常类型={}, 原因={}",
                        cache != null ? cache.getName() : "unknown",
                        exception.getClass().getSimpleName(), exception.getMessage());
            }
        };
    }
}
