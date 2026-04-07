package com.houseleasing.security;

import com.houseleasing.entity.User;
import com.houseleasing.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户详情服务实现类
 *
 * @author hongwenhao
 * @description 实现 Spring Security 的 UserDetailsService 接口，
 *              通过用户名从数据库加载用户信息用于认证，
 *              角色转换为 Spring Security 的 GrantedAuthority 格式（ROLE_前缀）
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    /**
     * 根据用户名或手机号从数据库加载用户认证信息
     *
     * @param usernameOrPhone 用户名或手机号
     * @return Spring Security UserDetails 对象（包含用户名、密码和权限）
     * @throws UsernameNotFoundException 用户不存在时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String usernameOrPhone) throws UsernameNotFoundException {
        // 为了兼容历史 Token 与手机号登录，这里支持三类输入：
        // 1) uid:123（新版 JWT Subject，稳定标识，用户名变更后仍可识别同一用户）
        // 2) username（用户名登录或旧版 Token）
        // 3) phone（手机号登录）
        User user = resolveUser(usernameOrPhone);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + usernameOrPhone);
        }
        // 将系统角色（如 TENANT/LANDLORD/ADMIN）转换为 Spring Security 的 ROLE_前缀格式
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }

    /**
     * 根据认证标识解析用户：
     * - 若为 uid: 前缀，按主键 ID 查询（稳定且不受用户名修改影响）；
     * - 否则回退到“用户名/手机号”双通道查询，兼容历史行为。
     *
     * @param identifier 认证标识
     * @return 命中的用户，未命中返回 null
     */
    private User resolveUser(String identifier) {
        if (identifier != null && identifier.startsWith(JwtSubjectConstants.USER_ID_SUBJECT_PREFIX)) {
            String userIdText = identifier.substring(JwtSubjectConstants.USER_ID_SUBJECT_PREFIX.length());
            try {
                Long userId = Long.valueOf(userIdText);
                return userMapper.selectById(userId);
            } catch (NumberFormatException ex) {
                throw new UsernameNotFoundException("无效的用户标识: " + identifier);
            }
        }
        return userMapper.selectByUsernameOrPhone(identifier);
    }
}
