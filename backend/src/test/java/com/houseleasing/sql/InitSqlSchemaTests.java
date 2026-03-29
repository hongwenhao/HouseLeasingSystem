package com.houseleasing.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证初始化 SQL 与后端合同状态常量保持一致，避免运行期写入状态时发生枚举截断。
 */
class InitSqlSchemaTests {

    @Test
    void contractsStatusEnumShouldContainSignedAndExcludeLegacyValues() throws IOException {
        Path initSqlPath = resolveInitSqlPath();
        String initSql = Files.readString(initSqlPath);

        assertTrue(
                initSql.contains("`status` ENUM('DRAFT','PENDING_SIGN','SIGNED','CANCELLED')"),
                "contracts.status 枚举应包含 SIGNED 并与后端状态一致"
        );
        assertFalse(initSql.contains("TENANT_SIGNED"), "contracts.status 不应再包含 TENANT_SIGNED");
        assertFalse(initSql.contains("LANDLORD_SIGNED"), "contracts.status 不应再包含 LANDLORD_SIGNED");
        assertFalse(initSql.contains("FULLY_SIGNED"), "contracts.status 不应再包含 FULLY_SIGNED");
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
