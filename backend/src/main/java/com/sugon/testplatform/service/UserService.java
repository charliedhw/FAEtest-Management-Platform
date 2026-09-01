package com.sugon.testplatform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sugon.testplatform.common.PageResult;
import com.sugon.testplatform.dto.LoginRequest;
import com.sugon.testplatform.dto.LoginResponse;
import com.sugon.testplatform.dto.UserSaveRequest;
import com.sugon.testplatform.entity.SysUser;

import java.util.List;

public interface UserService {
    LoginResponse login(LoginRequest req);
    List<String> getRoleCodes(Long userId);
    PageResult<SysUser> page(int pageNum, int pageSize, String keyword);
    void save(UserSaveRequest req);
    void deleteUser(Long userId);
    java.util.Map<String, Object> importUsers(org.springframework.web.multipart.MultipartFile file);
    void changePassword(Long userId, String oldPwd, String newPwd);
    void resetPassword(Long userId);
    List<SysUser> listByRole(String roleCode);
    List<SysUser> listAll();
    List<Long> getUserRoleIds(Long userId);
    List<com.sugon.testplatform.entity.SysRole> listAllRoles();
}
