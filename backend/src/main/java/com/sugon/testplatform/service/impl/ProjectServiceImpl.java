package com.sugon.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sugon.testplatform.common.BizException;
import com.sugon.testplatform.common.PageResult;
import com.sugon.testplatform.entity.TestApplication;
import com.sugon.testplatform.entity.TestProgress;
import com.sugon.testplatform.entity.TestProject;
import com.sugon.testplatform.entity.TestReport;
import com.sugon.testplatform.mapper.TestApplicationMapper;
import com.sugon.testplatform.mapper.TestProgressMapper;
import com.sugon.testplatform.mapper.TestProjectMapper;
import com.sugon.testplatform.mapper.TestReportMapper;
import com.sugon.testplatform.security.UserContext;
import com.sugon.testplatform.service.ProjectService;
import com.sugon.testplatform.service.ReportService;
import com.sugon.testplatform.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ProjectServiceImpl implements ProjectService {
    private final TestProjectMapper projectMapper;
    private final TestProgressMapper progressMapper;
    private final TestReportMapper reportMapper;
    private final TestApplicationMapper applicationMapper;
    private final ReportService reportService;
    private final ResourceService resourceService;

    public ProjectServiceImpl(TestProjectMapper projectMapper, TestProgressMapper progressMapper,
                              TestReportMapper reportMapper, TestApplicationMapper applicationMapper,
                              ReportService reportService, @Lazy ResourceService resourceService) {
        this.projectMapper = projectMapper;
        this.progressMapper = progressMapper;
        this.reportMapper = reportMapper;
        this.applicationMapper = applicationMapper;
        this.reportService = reportService;
        this.resourceService = resourceService;
    }

    @Override
    public PageResult<TestProject> page(int pageNum, int pageSize, Map<String, Object> params) {
        Page<TestProject> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TestProject> qw = buildQuery(params);
        applyProjectDataScope(qw);
        qw.orderByDesc(TestProject::getUpdateTime);
        Page<TestProject> result = projectMapper.selectPage(page, qw);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    /**
     * 项目数据权限
     * 审批组(APPROVER)/领导(LEADER)/管理员(ADMIN)/资源管理员: 全部
     * 售前(PRESALES): presales_id是自己 或 自己创建
     * 销售(SALES): sales_id是自己
     * 测试(TESTER): tester_ids包含自己
     * 其他: 自己创建
     */
    private void applyProjectDataScope(LambdaQueryWrapper<TestProject> qw) {
        java.util.List<String> roles = com.sugon.testplatform.security.UserContext.getRoles();
        Long uid = com.sugon.testplatform.security.UserContext.getUserId();
        if (uid == null) {
            qw.eq(TestProject::getId, -1);
            return;
        }
        boolean seeAll = roles.contains("ADMIN") || roles.contains("APPROVER")
                || roles.contains("LEADER") || roles.contains("RESOURCE_ADMIN")
                || roles.contains("FAE_LEADER");
        if (seeAll) return;

        qw.and(w -> {
            if (roles.contains("PRESALES")) {
                w.eq(TestProject::getPresalesId, uid).or();
            }
            if (roles.contains("SALES")) {
                w.eq(TestProject::getSalesId, uid).or();
            }
            if (roles.contains("TESTER")) {
                // tester_ids逗号分隔，用4种位置精确匹配uid
                String u = uid.toString();
                w.apply("(tester_ids = {0} OR tester_ids LIKE CONCAT({0},',%') OR tester_ids LIKE CONCAT('%,',{0}) OR tester_ids LIKE CONCAT('%,',{0},',%'))", u).or();
            }
            w.eq(TestProject::getCreateBy, uid);
        });
    }

    private LambdaQueryWrapper<TestProject> buildQuery(Map<String, Object> params) {
        LambdaQueryWrapper<TestProject> qw = new LambdaQueryWrapper<>();
        if (params == null) return qw;
        Object status = params.get("status");
        if (status != null && StringUtils.hasText(status.toString())) {
            qw.eq(TestProject::getStatus, status);
        }
        Object region = params.get("region");
        if (region != null && StringUtils.hasText(region.toString())) {
            qw.eq(TestProject::getRegion, region);
        }
        Object testType = params.get("testType");
        if (testType != null && StringUtils.hasText(testType.toString())) {
            qw.like(TestProject::getTestType, testType);
        }
        // 测试人员(姓名模糊)
        Object tester = params.get("tester");
        if (tester != null && StringUtils.hasText(tester.toString())) {
            qw.like(TestProject::getTesterNames, tester);
        }
        // 测试设备
        Object deviceType = params.get("deviceType");
        if (deviceType != null && StringUtils.hasText(deviceType.toString())) {
            qw.like(TestProject::getDeviceType, deviceType);
        }
        // 测试周期分段: LE7(<=7天) / 8-15 / 16-30 / 31-90 / GT90(>90天)
        Object period = params.get("period");
        if (period != null && StringUtils.hasText(period.toString())) {
            String p = period.toString();
            qw.isNotNull(TestProject::getTestStartTime).isNotNull(TestProject::getTestEndTime);
            String dayExpr = "DATEDIFF(test_end_time, test_start_time)";
            switch (p) {
                case "LE7" -> qw.apply(dayExpr + " <= 7");
                case "8-15" -> qw.apply(dayExpr + " BETWEEN 8 AND 15");
                case "16-30" -> qw.apply(dayExpr + " BETWEEN 16 AND 30");
                case "31-90" -> qw.apply(dayExpr + " BETWEEN 31 AND 90");
                case "GT90" -> qw.apply(dayExpr + " > 90");
                default -> { }
            }
        }
        // 测试开始时间范围(自定义)
        Object startFrom = params.get("testStartFrom");
        if (startFrom != null && StringUtils.hasText(startFrom.toString())) {
            qw.ge(TestProject::getTestStartTime, startFrom);
        }
        Object startTo = params.get("testStartTo");
        if (startTo != null && StringUtils.hasText(startTo.toString())) {
            qw.le(TestProject::getTestStartTime, startTo);
        }
        Object keyword = params.get("keyword");
        if (keyword != null && StringUtils.hasText(keyword.toString())) {
            qw.and(w -> w.like(TestProject::getProjectName, keyword)
                    .or().like(TestProject::getCustomerName, keyword)
                    .or().like(TestProject::getProjectNo, keyword)
                    .or().like(TestProject::getSpmNo, keyword));
        }
        Object salesId = params.get("salesId");
        if (salesId != null) qw.eq(TestProject::getSalesId, salesId);
        Object presalesId = params.get("presalesId");
        if (presalesId != null) qw.eq(TestProject::getPresalesId, presalesId);
        Object isKey = params.get("isKeyProject");
        if (isKey != null) qw.eq(TestProject::getIsKeyProject, isKey);
        Object bizType = params.get("bizType");
        if (bizType != null && StringUtils.hasText(bizType.toString())) {
            qw.eq(TestProject::getBizType, bizType);
        }
        return qw;
    }

    @Override
    public TestProject detail(Long id) {
        TestProject project = projectMapper.selectById(id);
        if (project != null) {
            project.setPermissions(calcPermissions(project));
        }
        return project;
    }

    /**
     * 计算当前用户对该项目的操作权限
     * startTest(开始测试): 管理员 + 被分配的FAE测试人员
     * updateBid(更新中标): 所有人
     * setKey(设为重点): 管理员 + 测试审批组
     * editProgress(填进展/传报告): 被分配的FAE测试人员 + 管理员
     */
    public java.util.Map<String, Boolean> calcPermissions(TestProject project) {
        java.util.List<String> roles = UserContext.getRoles();
        Long uid = UserContext.getUserId();
        boolean isAdmin = roles.contains("ADMIN");
        boolean isApprover = roles.contains("APPROVER");
        boolean isAssignedTester = isAssignedTester(project, uid);

        java.util.Map<String, Boolean> perm = new java.util.HashMap<>();
        perm.put("startTest", isAdmin || isAssignedTester);
        perm.put("updateBid", true);
        perm.put("setKey", isAdmin || isApprover);
        perm.put("editProgress", isAdmin || isAssignedTester);
        perm.put("delete", isAdmin || roles.contains("RESOURCE_ADMIN") || roles.contains("FAE_LEADER"));
        return perm;
    }

    /**
     * 当前用户是否是被分配到该项目的测试人员
     */
    private boolean isAssignedTester(TestProject project, Long uid) {
        if (uid == null || project.getTesterIds() == null || project.getTesterIds().isEmpty()) {
            return false;
        }
        String uidStr = uid.toString();
        for (String tid : project.getTesterIds().split(",")) {
            if (uidStr.equals(tid.trim())) return true;
        }
        return false;
    }

    /**
     * 校验当前用户是否有开始测试/改状态权限
     */
    public void checkStartTestPermission(Long id) {
        TestProject project = projectMapper.selectById(id);
        if (project == null) throw new BizException("项目不存在");
        java.util.Map<String, Boolean> perm = calcPermissions(project);
        if (!perm.get("startTest")) {
            throw new BizException("只有管理员或被分配的FAE测试人员才能操作测试状态");
        }
    }

    /**
     * 校验设为重点权限
     */
    public void checkSetKeyPermission() {
        java.util.List<String> roles = UserContext.getRoles();
        if (!roles.contains("ADMIN") && !roles.contains("APPROVER")) {
            throw new BizException("只有管理员或测试审批组才能设置重点项目");
        }
    }

    /**
     * 当前用户是否能编辑该项目进展/上传报告(管理员+被分配FAE测试)
     */
    @Override
    public boolean canEditProgress(Long projectId) {
        TestProject project = projectMapper.selectById(projectId);
        if (project == null) return false;
        java.util.List<String> roles = UserContext.getRoles();
        if (roles.contains("ADMIN")) return true;
        return isAssignedTester(project, UserContext.getUserId());
    }

    @Override
    public void update(TestProject project) {
        if (project.getId() == null) throw new BizException("项目ID不能为空");
        // 如果变更了"是否重点"字段,校验权限(管理员+测试审批组)
        if (project.getIsKeyProject() != null) {
            TestProject old = projectMapper.selectById(project.getId());
            if (old != null && !project.getIsKeyProject().equals(old.getIsKeyProject())) {
                checkSetKeyPermission();
            }
        }
        projectMapper.updateById(project);
    }

    @Override
    public void updateStatus(Long id, String status) {
        // 开始/暂停/恢复/完成 等状态操作需要权限
        checkStartTestPermission(id);
        TestProject p = new TestProject();
        p.setId(id);
        p.setStatus(status);
        projectMapper.updateById(p);
        // 项目完成或关闭时,自动回收名下借用的资源
        if ("COMPLETED".equals(status) || "CLOSED".equals(status)) {
            int recycled = resourceService.recycleByProject(id);
            if (recycled > 0) {
                log.info("项目[{}]完成/关闭,自动回收资源 {} 个", id, recycled);
            }
        }
    }

    @Override
    public void updateBid(Long id, String bidStatus, BigDecimal bidAmount) {
        TestProject p = new TestProject();
        p.setId(id);
        p.setBidStatus(bidStatus);
        p.setBidAmount(bidAmount);
        projectMapper.updateById(p);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TestProject project = projectMapper.selectById(id);
        if (project == null) throw new BizException("项目不存在");
        // 只有管理员/资源管理员/FAE负责人可删除
        java.util.List<String> roles = UserContext.getRoles();
        boolean canDelete = roles.contains("ADMIN") || roles.contains("RESOURCE_ADMIN") || roles.contains("FAE_LEADER");
        if (!canDelete) {
            throw new BizException("无权限删除项目，请联系管理员");
        }

        // 级联删除进展
        progressMapper.delete(new LambdaQueryWrapper<TestProgress>().eq(TestProgress::getProjectId, id));
        // 级联删除报告(含MinIO文件)
        List<TestReport> reports = reportMapper.selectList(
                new LambdaQueryWrapper<TestReport>().eq(TestReport::getProjectId, id));
        for (TestReport r : reports) {
            try {
                reportService.delete(r.getId());
            } catch (Exception ignored) {
            }
        }
        // 解除申请单关联(用实体UpdateWrapper显式set null)
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<TestApplication> uw =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        uw.eq(TestApplication::getProjectId, id).set(TestApplication::getProjectId, null);
        applicationMapper.update(null, uw);
        // 删除项目
        projectMapper.deleteById(id);
    }
}
