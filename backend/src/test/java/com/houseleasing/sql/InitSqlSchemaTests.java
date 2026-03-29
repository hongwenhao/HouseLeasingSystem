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
    void contractsStatusEnumShouldMatchCurrentContractFlow() throws IOException {
        Path initSqlPath = resolveInitSqlPath();
        String initSql = Files.readString(initSqlPath);

        assertTrue(
                initSql.contains("`status` ENUM('DRAFT','PENDING_SIGN','TENANT_SIGNED','LANDLORD_SIGNED','FULLY_SIGNED','CANCELLED')"),
                "contracts.status 枚举应与合同签署流程状态一致"
        );
        assertFalse(
                initSql.contains("`status` ENUM('DRAFT','PENDING_SIGN','SIGNED','CANCELLED')"),
                "contracts.status 不应再包含旧的四态枚举定义"
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
