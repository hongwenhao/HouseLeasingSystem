package com.houseleasing.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 users 表包含 gender 字段，确保个人中心性别功能与数据库结构一致。
 */
class UserSchemaGenderTests {

    @Test
    void usersTableShouldContainGenderColumn() throws IOException {
        Path initSqlPath = resolveInitSqlPath();
        String initSql = Files.readString(initSqlPath);

        assertTrue(
                initSql.contains("`gender` TINYINT DEFAULT 0"),
                "users 表应包含 gender 字段并提供默认值"
        );
    }

    private Path resolveInitSqlPath() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            Path candidate = current.resolve("sql").resolve("init.sql");
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("无法定位 sql/init.sql");
    }
}
