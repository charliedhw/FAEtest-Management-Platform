package com.sugon.testplatform.controller;

import com.sugon.testplatform.common.BizException;
import com.sugon.testplatform.common.PageResult;
import com.sugon.testplatform.common.Result;
import com.sugon.testplatform.entity.Resource;
import com.sugon.testplatform.entity.ResourceLoan;
import com.sugon.testplatform.security.UserContext;
import com.sugon.testplatform.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/resource")
@RequiredArgsConstructor
public class ResourceController {
    private final ResourceService resourceService;

    /**
     * 资源管理模块权限: 测试审批组/FAE负责人/资源管理员/管理员
     */
    private void checkPerm() {
        List<String> roles = UserContext.getRoles();
        boolean allowed = roles.contains("APPROVER") || roles.contains("FAE_LEADER")
                || roles.contains("RESOURCE_ADMIN") || roles.contains("ADMIN");
        if (!allowed) {
            throw new BizException("无权限访问资源管理模块");
        }
    }

    @GetMapping("/page")
    public Result<PageResult<Resource>> page(@RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "10") int pageSize,
                                             @RequestParam(required = false) String type,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) String keyword) {
        checkPerm();
        return Result.ok(resourceService.pageResource(pageNum, pageSize, type, status, keyword));
    }

    @PostMapping("/save")
    public Result<Void> save(@RequestBody Resource resource) {
        checkPerm();
        resourceService.saveResource(resource);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        checkPerm();
        resourceService.deleteResource(id);
        return Result.ok();
    }

    @PostMapping("/online/{id}")
    public Result<Void> online(@PathVariable Long id) {
        checkPerm();
        resourceService.online(id);
        return Result.ok();
    }

    @PostMapping("/offline/{id}")
    public Result<Void> offline(@PathVariable Long id) {
        checkPerm();
        resourceService.offline(id);
        return Result.ok();
    }

    /**
     * 资产中心: 销售/售前/所有登录用户可查看已上线资产(只读)
     * 注意: 不调用 checkPerm(), 对所有登录用户开放, 仅返回已上线资产
     */
    @GetMapping("/asset/page")
    public Result<PageResult<Resource>> assetPage(@RequestParam(defaultValue = "1") int pageNum,
                                                  @RequestParam(defaultValue = "10") int pageSize,
                                                  @RequestParam(required = false) String type,
                                                  @RequestParam(required = false) String keyword) {
        return Result.ok(resourceService.pageOnlineAsset(pageNum, pageSize, type, keyword));
    }

    @PostMapping("/borrow")
    public Result<ResourceLoan> borrow(@RequestBody ResourceLoan loan) {
        checkPerm();
        return Result.ok(resourceService.borrow(loan));
    }

    @PostMapping("/return/{loanId}")
    public Result<Void> returnResource(@PathVariable Long loanId) {
        checkPerm();
        resourceService.returnResource(loanId);
        return Result.ok();
    }

    @GetMapping("/loan/page")
    public Result<PageResult<ResourceLoan>> loanPage(@RequestParam(defaultValue = "1") int pageNum,
                                                     @RequestParam(defaultValue = "10") int pageSize,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(required = false) Long borrowerId) {
        checkPerm();
        Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        params.put("borrowerId", borrowerId);
        return Result.ok(resourceService.pageLoan(pageNum, pageSize, params));
    }

    @GetMapping("/overdue")
    public Result<List<ResourceLoan>> overdue() {
        checkPerm();
        return Result.ok(resourceService.overdueList());
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        checkPerm();
        return Result.ok(resourceService.resourceStats());
    }
}
