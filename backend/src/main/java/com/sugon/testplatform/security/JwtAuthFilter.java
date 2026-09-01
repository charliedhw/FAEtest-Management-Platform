package com.sugon.testplatform.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sugon.testplatform.entity.SysRole;
import com.sugon.testplatform.entity.SysUser;
import com.sugon.testplatform.entity.SysUserRole;
import com.sugon.testplatform.mapper.SysRoleMapper;
import com.sugon.testplatform.mapper.SysUserMapper;
import com.sugon.testplatform.mapper.SysUserRoleMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        String token = req.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                Claims claims = jwtUtil.parse(token);
                Long userId = Long.valueOf(claims.getSubject());
                LoginUser user = buildLoginUser(userId, claims);
                UserContext.set(user);
            } catch (Exception ignored) {
            }
        }
        try {
            chain.doFilter(req, resp);
        } finally {
            UserContext.clear();
        }
    }

    /**
     * 构建登录用户：优先Redis缓存，miss时从DB回源并回填缓存
     */
    private LoginUser buildLoginUser(Long userId, Claims claims) {
        LoginUser user = new LoginUser();
        user.setUserId(userId);
        user.setUsername(claims.get("username", String.class));

        String realName = null;
        List<String> roles = null;
        try {
            realName = redis.opsForValue().get("login:name:" + userId);
            String rolesJson = redis.opsForValue().get("login:roles:" + userId);
            if (rolesJson != null) {
                roles = objectMapper.readValue(rolesJson,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            }
        } catch (Exception ignored) {
        }

        // 缓存缺失时从数据库回源
        if (realName == null || roles == null) {
            SysUser dbUser = userMapper.selectById(userId);
            if (dbUser != null) {
                if (realName == null || realName.isEmpty()) realName = dbUser.getRealName();
                if (roles == null) roles = loadRolesFromDb(userId);
                try {
                    redis.opsForValue().set("login:name:" + userId,
                            realName == null ? "" : realName, 12, TimeUnit.HOURS);
                    redis.opsForValue().set("login:roles:" + userId,
                            objectMapper.writeValueAsString(roles), 12, TimeUnit.HOURS);
                } catch (Exception ignored) {
                }
            }
        }
        user.setRealName(realName);
        user.setRoles(roles == null ? List.of() : roles);
        return user;
    }

    private List<String> loadRolesFromDb(Long userId) {
        List<SysUserRole> urs = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (urs.isEmpty()) return List.of();
        List<Long> roleIds = urs.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        return roleMapper.selectBatchIds(roleIds).stream()
                .map(SysRole::getRoleCode).collect(Collectors.toList());
    }
}
