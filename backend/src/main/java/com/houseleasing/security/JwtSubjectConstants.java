package com.houseleasing.security;

/**
 * JWT Subject 约定常量。
 * <p>
 * 统一维护 Subject 前缀，避免生成与解析两端写死不同字符串导致认证失败。
 */
public final class JwtSubjectConstants {

    private JwtSubjectConstants() {
    }

    /** Subject 前缀：表示后续值为用户主键 ID。 */
    public static final String USER_ID_SUBJECT_PREFIX = "uid:";
}
