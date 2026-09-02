package com.sugon.testplatform.controller;

import com.sugon.testplatform.common.PageResult;
import com.sugon.testplatform.common.Result;
import com.sugon.testplatform.dto.UserSaveRequest;
import com.sugon.testplatform.entity.SysUser;
import com.sugon.testplatform.security.UserContext;
import com.sugon.testplatform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/page")
    public Result<PageResult<SysUser>> page(@RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize,
                                            @RequestParam(required = false) String keyword) {
        return Result.ok(userService.page(pageNum, pageSize, keyword));
    }

    @PostMapping("/save")
    public Result<Void> save(@RequestBody UserSaveRequest req) {
        userService.save(req);
        return Result.ok();
    }

    @PostMapping("/changePassword")
    public Result<Void> changePassword(@RequestBody Map<String, String> body) {
        userService.changePassword(UserContext.getUserId(), body.get("oldPassword"), body.get("newPassword"));
        return Result.ok();
    }

    @PostMapping("/resetPassword/{userId}")
    public Result<Void> resetPassword(@PathVariable Long userId) {
        userService.resetPassword(userId);
        return Result.ok();
    }

    @DeleteMapping("/{userId}")
    public Result<Void> delete(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return Result.ok();
    }

    /**
     * 批量删除用户（跳过admin和当前登录人）
     */
    @PostMapping("/batchDelete")
    public Result<Integer> batchDelete(@RequestBody List<Long> userIds) {
        return Result.ok(userService.batchDelete(userIds));
    }

    /**
     * 批量重置密码为默认密码
     */
    @PostMapping("/batchResetPassword")
    public Result<Integer> batchResetPassword(@RequestBody List<Long> userIds) {
        return Result.ok(userService.batchResetPassword(userIds));
    }

    /**
     * 导入人员Excel生成账号并归组
     */
    @PostMapping("/import")
    public Result<Map<String, Object>> importUsers(@RequestParam MultipartFile file) {
        return Result.ok(userService.importUsers(file));
    }

    @GetMapping("/listByRole")
    public Result<List<SysUser>> listByRole(@RequestParam String roleCode) {
        return Result.ok(userService.listByRole(roleCode));
    }

    @GetMapping("/listAll")
    public Result<List<SysUser>> listAll() {
        return Result.ok(userService.listAll());
    }

    @GetMapping("/info")
    public Result<Object> info() {
        return Result.ok(UserContext.get());
    }

    @GetMapping("/roles")
    public Result<Object> allRoles() {
        return Result.ok(userService.listAllRoles());
    }

    @GetMapping("/roleIds/{userId}")
    public Result<List<Long>> userRoleIds(@PathVariable Long userId) {
        return Result.ok(userService.getUserRoleIds(userId));
    }
}
