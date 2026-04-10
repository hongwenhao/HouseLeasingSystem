package com.houseleasing.config;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserSchemaCompatibilityInitializerTest {

    @Test
    void shouldAlterIdCardColumnWhenLengthTooShort() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(18);
        UserSchemaCompatibilityInitializer initializer = new UserSchemaCompatibilityInitializer(jdbcTemplate);

        initializer.run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).execute(sqlCaptor.capture());
        assertTrue(sqlCaptor.getValue().contains("MODIFY COLUMN `id_card` VARCHAR(255)"));
    }

    @Test
    void shouldNotAlterIdCardColumnWhenLengthIsEnough() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(255);
        UserSchemaCompatibilityInitializer initializer = new UserSchemaCompatibilityInitializer(jdbcTemplate);

        initializer.run(new DefaultApplicationArguments(new String[0]));

        verify(jdbcTemplate, never()).execute(anyString());
    }
}
