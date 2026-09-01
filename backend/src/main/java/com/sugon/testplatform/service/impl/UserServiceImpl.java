package com.sugon.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sugon.testplatform.common.BizException;
import com.sugon.testplatform.common.PageResult;
import com.sugon.testplatform.dto.LoginRequest;
import com.sugon.testplatform.dto.LoginResponse;
import com.sugon.testplatform.dto.UserSaveRequest;
import com.sugon.testplatform.entity.SysRole;
import com.sugon.testplatform.entity.SysUser;
import com.sugon.testplatform.entity.SysUserGroup;
import com.sugon.testplatform.entity.SysUserGroupRel;
import com.sugon.testplatform.entity.SysUserRole;
import com.sugon.testplatform.mapper.SysRoleMapper;
import com.sugon.testplatform.mapper.SysUserGroupMapper;
import com.sugon.testplatform.mapper.SysUserGroupRelMapper;
import com.sugon.testplatform.mapper.SysUserMapper;
import com.sugon.testplatform.mapper.SysUserRoleMapper;
import com.sugon.testplatform.security.JwtUtil;
import com.sugon.testplatform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserGroupMapper userGroupMapper;
    private final SysUserGroupRelMapper userGroupRelMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String DEFAULT_PWD = "Sugon@123";

    @Override
    public LoginResponse login(LoginRequest req) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.getUsername()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException("账号已被停用");
        }
        List<String> roles = getRoleCodes(user.getId());
        String token = jwtUtil.createToken(user.getId(), user.getUsername());
        try {
            redis.opsForValue().set("login:roles:" + user.getId(),
                    objectMapper.writeValueAsString(roles), 12, TimeUnit.HOURS);
            redis.opsForValue().set("login:name:" + user.getId(),
                    user.getRealName() == null ? "" : user.getRealName(), 12, TimeUnit.HOURS);
        } catch (Exception ignored) {
        }
        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        resp.setRoles(roles);
        resp.setIsFirstLogin(user.getIsFirstLogin());
        return resp;
    }

    @Override
    public List<String> getRoleCodes(Long userId) {
        List<SysUserRole> urs = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (urs.isEmpty()) return List.of();
        List<Long> roleIds = urs.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        return roleMapper.selectBatchIds(roleIds).stream()
                .map(SysRole::getRoleCode).collect(Collectors.toList());
    }

    @Override
    public PageResult<SysUser> page(int pageNum, int pageSize, String keyword) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword);
        }
        qw.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> result = userMapper.selectPage(page, qw);
        result.getRecords().forEach(u -> u.setPassword(null));
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    @Override
    @Transactional
    public void save(UserSaveRequest req) {
        SysUser user;
        if (req.getId() == null) {
            // create
            Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, req.getUsername()));
            if (count > 0) throw new BizException("用户名已存在");
            user = new SysUser();
            user.setUsername(req.getUsername());
            String pwd = StringUtils.hasText(req.getPassword()) ? req.getPassword() : DEFAULT_PWD;
            user.setPassword(passwordEncoder.encode(pwd));
            user.setIsFirstLogin(1);
        } else {
            user = userMapper.selectById(req.getId());
            if (user == null) throw new BizException("用户不存在");
            if (StringUtils.hasText(req.getPassword())) {
                user.setPassword(passwordEncoder.encode(req.getPassword()));
            }
        }
        user.setRealName(req.getRealName());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setDeptId(req.getDeptId());
        user.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        if (req.getId() == null) {
            userMapper.insert(user);
        } else {
            userMapper.updateById(user);
        }
        // roles
        if (req.getRoleIds() != null) {
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getUserId, user.getId()));
            for (Long roleId : req.getRoleIds()) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(user.getId());
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BizException("用户不存在");
        if ("admin".equals(user.getUsername())) throw new BizException("不能删除管理员账号");
        // 删除用户角色关联
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        userMapper.deleteById(userId);
    }

    @Override
    public void changePassword(Long userId, String oldPwd, String newPwd) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BizException("用户不存在");
        if (!passwordEncoder.matches(oldPwd, user.getPassword())) {
            throw new BizException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPwd));
        user.setIsFirstLogin(0);
        userMapper.updateById(user);
    }

    @Override
    public void resetPassword(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BizException("用户不存在");
        user.setPassword(passwordEncoder.encode(DEFAULT_PWD));
        user.setIsFirstLogin(1);
        userMapper.updateById(user);
    }

    @Override
    public List<SysUser> listByRole(String roleCode) {
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleCode));
        if (role == null) return List.of();
        List<SysUserRole> urs = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, role.getId()));
        if (urs.isEmpty()) return List.of();
        List<Long> userIds = urs.stream().map(SysUserRole::getUserId).collect(Collectors.toList());
        List<SysUser> users = userMapper.selectBatchIds(userIds);
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    @Override
    public List<SysUser> listAll() {
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1));
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    @Override
    public List<Long> getUserRoleIds(Long userId) {
        List<SysUserRole> urs = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
        return urs.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
    }

    @Override
    public List<SysRole> listAllRoles() {
        return roleMapper.selectList(null);
    }

    // 角色/用户组映射
    private static final Map<String, String[]> GROUP_ROLE_MAP = Map.of(
            "售前组", new String[]{"PRESALES", "PRESALES_GROUP"},
            "销售组", new String[]{"SALES", "SALES_GROUP"},
            "FAE测试组", new String[]{"TESTER", "FAE_GROUP"},
            "测试审批组", new String[]{"APPROVER", "APPROVER_GROUP"}
    );
    // 销售组默认密码
    private static final String SALES_DEFAULT_PWD = "Sugon@cre**";

    @Override
    @Transactional
    public Map<String, Object> importUsers(org.springframework.web.multipart.MultipartFile file) {
        int created = 0, skipped = 0;
        List<String> errors = new ArrayList<>();
        List<String> details = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            // 找"人员名录"sheet
            Sheet sheet = null;
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                if (wb.getSheetName(i).contains("人员名录")) {
                    sheet = wb.getSheetAt(i);
                    break;
                }
            }
            if (sheet == null) {
                throw new BizException("未找到「人员名录」工作表");
            }
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String name = getCellVal(row.getCell(0));
                String groupName = getCellVal(row.getCell(1));
                String phone = getCellVal(row.getCell(2));
                String email = getCellVal(row.getCell(3));
                if (!StringUtils.hasText(name) || !StringUtils.hasText(email)) continue;

                String username = email.split("@")[0].trim();
                // 检查是否已存在
                Long exist = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username));
                if (exist > 0) {
                    skipped++;
                    details.add(name + "(" + username + ") 已存在,跳过");
                    continue;
                }
                String[] mapping = GROUP_ROLE_MAP.get(groupName);
                if (mapping == null) {
                    errors.add(name + " 角色组[" + groupName + "]无法识别,跳过");
                    skipped++;
                    continue;
                }
                // 销售组默认密码 Sugon@cre**, 其他组默认 Sugon@123
                String pwd = "销售组".equals(groupName) ? SALES_DEFAULT_PWD : DEFAULT_PWD;

                SysUser user = new SysUser();
                user.setUsername(username);
                user.setPassword(passwordEncoder.encode(pwd));
                user.setRealName(name);
                user.setEmail(email);
                user.setPhone(phone);
                user.setStatus(1);
                user.setIsFirstLogin(1);
                userMapper.insert(user);

                // 分配角色
                SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleCode, mapping[0]));
                if (role != null) {
                    SysUserRole ur = new SysUserRole();
                    ur.setUserId(user.getId());
                    ur.setRoleId(role.getId());
                    userRoleMapper.insert(ur);
                }
                // 归入用户组
                SysUserGroup group = userGroupMapper.selectOne(new LambdaQueryWrapper<SysUserGroup>()
                        .eq(SysUserGroup::getGroupCode, mapping[1]));
                if (group != null) {
                    SysUserGroupRel rel = new SysUserGroupRel();
                    rel.setUserId(user.getId());
                    rel.setGroupId(group.getId());
                    userGroupRelMapper.insert(rel);
                }
                created++;
                details.add(name + "(" + username + ") -> " + groupName);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("解析Excel失败: " + e.getMessage());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("errors", errors);
        result.put("details", details);
        return result;
    }

    private String getCellVal(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return null;
        cell.setCellType(org.apache.poi.ss.usermodel.CellType.STRING);
        String v = cell.getStringCellValue();
        return v == null ? null : v.trim();
    }
}
