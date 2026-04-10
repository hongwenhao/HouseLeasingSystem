package com.houseleasing.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 兼容历史库结构：确保 users.id_card 字段长度足够存储加密后的身份证密文。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSchemaCompatibilityInitializer implements ApplicationRunner {

    private static final int REQUIRED_ID_CARD_LENGTH = 255;
    private static final String ID_CARD_LENGTH_SQL = """
            SELECT CHARACTER_MAXIMUM_LENGTH
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'users'
              AND COLUMN_NAME = 'id_card'
            """;
    private static final String ALTER_ID_CARD_SQL = """
            ALTER TABLE `users`
            MODIFY COLUMN `id_card` VARCHAR(255) COMMENT '身份证号码（密文存储，应用层加密后写入）'
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        Integer currentLength;
        try {
            currentLength = jdbcTemplate.queryForObject(ID_CARD_LENGTH_SQL, Integer.class);
        } catch (EmptyResultDataAccessException ex) {
            log.warn("未找到 users.id_card 字段，跳过兼容性检查");
            return;
        } catch (DataAccessException ex) {
            log.warn("查询 users.id_card 字段长度失败，跳过兼容性检查：{}", ex.getMessage());
            return;
        }

        if (currentLength >= REQUIRED_ID_CARD_LENGTH) {
            return;
        }

        try {
            jdbcTemplate.execute(ALTER_ID_CARD_SQL);
            log.info("已自动扩展 users.id_card 字段长度：{} -> {}", currentLength, REQUIRED_ID_CARD_LENGTH);
        } catch (DataAccessException ex) {
            log.warn("自动扩展 users.id_card 字段失败，请手动执行 ALTER TABLE：{}", ex.getMessage());
        }
    }
}
