package com.houseleasing.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证初始化 SQL 与后端合同状态常量保持一致，避免运行期写入状态时发生枚举截断。
 */
class InitSqlSchemaTests {

    @Test
    void contractsStatusEnumShouldContainSignedAndExcludeLegacyValues() throws IOException {
        String initSql = Files.readString(Path.of("..", "sql", "init.sql"));

        assertTrue(
                initSql.contains("`status` ENUM('DRAFT','PENDING_SIGN','SIGNED','CANCELLED')"),
                "contracts.status 枚举应包含 SIGNED 并与后端状态一致"
        );
    }
}
