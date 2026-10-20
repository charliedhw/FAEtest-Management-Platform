package com.sugon.testplatform.service;

import com.sugon.testplatform.common.PageResult;
import com.sugon.testplatform.entity.TestProject;
import java.util.Map;

public interface ProjectService {
    PageResult<TestProject> page(int pageNum, int pageSize, Map<String, Object> params);
    TestProject detail(Long id);
    void update(TestProject project);
    void updateStatus(Long id, String status);
    void updateBid(Long id, String bidStatus, java.math.BigDecimal bidAmount);
    void delete(Long id);
    void checkStartTestPermission(Long id);
    void checkSetKeyPermission();
    boolean canEditProgress(Long projectId);
    // 当前用户是否有权查看该项目（数据范围）
    boolean canView(Long projectId);
    // 给项目查询追加当前用户的数据范围条件（导出/汇总等场景复用）
    void applyDataScope(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TestProject> qw);
    // QueryWrapper 版本（统计分组查询使用），规则与上面一致
    void applyDataScope(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TestProject> qw);
}
