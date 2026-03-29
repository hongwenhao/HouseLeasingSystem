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
 * @author HouseLeasingSystem开发团队
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
        // 为了兼容使用手机号登录的场景，这里同时支持用户名与手机号的查询
        User user = userMapper.selectByUsernameOrPhone(usernameOrPhone);
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
}
