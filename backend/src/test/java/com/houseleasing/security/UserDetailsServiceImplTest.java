package com.houseleasing.security;

import com.houseleasing.entity.User;
import com.houseleasing.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * UserDetailsServiceImpl 单元测试：
 * 验证新版 JWT Subject（uid:用户ID）与旧版用户名/手机号查询均可兼容。
 */
@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void shouldLoadUserByStableUserIdSubject() {
        User user = buildUser(11L, "newName", "TENANT");
        when(userMapper.selectById(11L)).thenReturn(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername("uid:11");

        assertEquals("newName", userDetails.getUsername());
        assertEquals("ROLE_TENANT", userDetails.getAuthorities().iterator().next().getAuthority());
        verify(userMapper, times(1)).selectById(11L);
        verify(userMapper, never()).selectByUsernameOrPhone(anyString());
    }

    @Test
    void shouldFallbackToUsernameOrPhoneForLegacyIdentifier() {
        User user = buildUser(12L, "user12", "LANDLORD");
        when(userMapper.selectByUsernameOrPhone("user12")).thenReturn(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername("user12");

        assertEquals("user12", userDetails.getUsername());
        assertEquals("ROLE_LANDLORD", userDetails.getAuthorities().iterator().next().getAuthority());
        verify(userMapper, times(1)).selectByUsernameOrPhone("user12");
        verify(userMapper, never()).selectById(anyLong());
    }

    private User buildUser(Long id, String username, String role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        user.setPassword("encoded-password");
        return user;
    }
}

