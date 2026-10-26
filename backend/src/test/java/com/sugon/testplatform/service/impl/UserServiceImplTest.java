package com.sugon.testplatform.service.impl;

import com.sugon.testplatform.common.BizException;
import com.sugon.testplatform.entity.SysUser;
import com.sugon.testplatform.mapper.SysRoleMapper;
import com.sugon.testplatform.mapper.SysUserGroupMapper;
import com.sugon.testplatform.mapper.SysUserGroupRelMapper;
import com.sugon.testplatform.mapper.SysUserMapper;
import com.sugon.testplatform.mapper.SysUserRoleMapper;
import com.sugon.testplatform.security.JwtUtil;
import com.sugon.testplatform.security.LoginUser;
import com.sugon.testplatform.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {
    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserServiceImpl service = new UserServiceImpl(
            userMapper, mock(SysRoleMapper.class), mock(SysUserRoleMapper.class),
            mock(SysUserGroupMapper.class), mock(SysUserGroupRelMapper.class),
            passwordEncoder, mock(JwtUtil.class), mock(StringRedisTemplate.class));

    @BeforeEach
    void setUp() {
        LoginUser admin = new LoginUser();
        admin.setUserId(999L);
        admin.setRoles(List.of("ADMIN"));
        UserContext.set(admin);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void singleResetUsesCustomPasswordAndRequiresFirstLoginChange() {
        SysUser user = user(1L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.encode("Custom@2026")).thenReturn("encoded-custom");
        service.resetPassword(1L, "Custom@2026");
        assertEquals("encoded-custom", user.getPassword());
        assertEquals(1, user.getIsFirstLogin());
        verify(userMapper).updateById(user);
    }

    @Test
    void singleResetWithoutPasswordKeepsDefaultBehavior() {
        SysUser user = user(1L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.encode("Sugon@123")).thenReturn("encoded-default");
        service.resetPassword(1L, null);
        assertEquals("encoded-default", user.getPassword());
        verify(passwordEncoder).encode("Sugon@123");
    }

    @Test
    void customResetRejectsShortAndOverlongPasswords() {
        when(userMapper.selectById(1L)).thenReturn(user(1L));
        assertThrows(BizException.class, () -> service.resetPassword(1L, "short"));
        BizException tooLong = assertThrows(BizException.class,
                () -> service.resetPassword(1L, "中".repeat(25)));
        assertTrue(tooLong.getMessage().contains("72字节"));
        verify(passwordEncoder, never()).encode(any());
        verify(userMapper, never()).updateById(any(SysUser.class));
    }

    @Test
    void batchResetAppliesCustomPasswordToExistingUsers() {
        SysUser first = user(1L);
        SysUser second = user(2L);
        when(userMapper.selectById(1L)).thenReturn(first);
        when(userMapper.selectById(2L)).thenReturn(second);
        when(userMapper.selectById(3L)).thenReturn(null);
        when(passwordEncoder.encode("Batch@2026")).thenReturn("encoded-batch");
        int count = service.batchResetPassword(List.of(1L, 2L, 3L), "Batch@2026");
        assertEquals(2, count);
        assertEquals("encoded-batch", first.getPassword());
        assertEquals("encoded-batch", second.getPassword());
        assertEquals(1, first.getIsFirstLogin());
        assertEquals(1, second.getIsFirstLogin());
        verify(userMapper, times(2)).updateById(any(SysUser.class));
    }

    private SysUser user(Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername("user" + id);
        user.setPassword("old");
        user.setIsFirstLogin(0);
        return user;
    }
}
