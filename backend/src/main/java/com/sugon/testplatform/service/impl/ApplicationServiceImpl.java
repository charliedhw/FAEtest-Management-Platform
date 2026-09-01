package com.sugon.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sugon.testplatform.common.BizException;
import com.sugon.testplatform.common.PageResult;
import com.sugon.testplatform.dto.ApplicationSubmitRequest;
import com.sugon.testplatform.dto.ApprovalRequest;
import com.sugon.testplatform.dto.AssignRequest;
import com.sugon.testplatform.entity.ApprovalRecord;
import com.sugon.testplatform.entity.TestApplication;
import com.sugon.testplatform.entity.TestProject;
import com.sugon.testplatform.mapper.ApprovalRecordMapper;
import com.sugon.testplatform.mapper.SysUserGroupMapper;
import com.sugon.testplatform.mapper.SysUserMapper;
import com.sugon.testplatform.mapper.TestApplicationMapper;
import com.sugon.testplatform.mapper.TestProjectMapper;
import com.sugon.testplatform.security.UserContext;
import com.sugon.testplatform.service.ApplicationService;
import com.sugon.testplatform.service.DictService;
import com.sugon.testplatform.service.NotifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {
    private final TestApplicationMapper applicationMapper;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final TestProjectMapper projectMapper;
    private final NotifyService notifyService;
    private final DictService dictService;
    private final SysUserMapper userMapper;
    private final SysUserGroupMapper userGroupMapper;

    // 节点常量
    public static final String NODE_PRESALES = "PRESALES_EVAL";     // 售前评估(已废弃，保留兼容)
    public static final String NODE_APPROVAL = "APPROVAL";          // 测试审批组审批
    public static final String NODE_LEADER = "LEADER_APPROVAL";     // 魏总审批
    public static final String NODE_ASSIGN = "ASSIGN";              // 分配资源(daihw)
    public static final String NODE_DONE = "DONE";

    // 状态常量
    public static final String ST_DRAFT = "DRAFT";
    public static final String ST_PRESALES = "PENDING_PRESALES";
    public static final String ST_APPROVAL = "PENDING_APPROVAL";
    public static final String ST_LEADER = "PENDING_LEADER";
    public static final String ST_ASSIGN = "PENDING_ASSIGN";
    public static final String ST_ASSIGNED = "ASSIGNED";
    public static final String ST_REJECTED = "REJECTED";
    public static final String ST_CLOSED = "CLOSED";

    private String genAppNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long count = applicationMapper.selectCount(new LambdaQueryWrapper<TestApplication>()
                .likeRight(TestApplication::getAppNo, "TA" + date));
        return "TA" + date + String.format("%03d", count + 1);
    }

    @Override
    @Transactional
    public Long submit(ApplicationSubmitRequest req) {
        Long userId = UserContext.requireUserId();
        String realName = UserContext.require().getRealName();
        // 只有售前角色能提交测试申请
        if (!UserContext.getRoles().contains("PRESALES") && !UserContext.getRoles().contains("ADMIN")) {
            throw new BizException("只有售前工程师才能提交测试申请");
        }
        // 暂停借用校验
        checkBorrowLimit(userId);

        TestApplication app = new TestApplication();
        BeanUtils.copyProperties(req, app);
        // 测试类型: JSON数组字符串转顿号分隔纯文本
        app.setTestType(normalizeTestType(req.getTestType()));
        app.setAppNo(genAppNo());
        app.setApplicantId(userId);
        app.setApplicantName(realName);
        // 售前提交后直接进入测试审批组审批
        app.setStatus(ST_APPROVAL);
        app.setCurrentNode(NODE_APPROVAL);
        applicationMapper.insert(app);

        record(app.getId(), NODE_APPROVAL, userId, realName, "SUBMIT", "售前发起测试申请");
        // 通知测试审批组 -> 跳转审批中心
        notifyService.sendToRole("APPROVER", "新的测试申请待审批",
                "售前【" + app.getApplicantName() + "】提交了测试申请【" + app.getProjectName() + "】，请及时审批。",
                "APPROVAL", app.getId(), "/approval");
        return app.getId();
    }

    private void checkBorrowLimit(Long userId) {
        // 规则引擎：暂停借用三条件（此处基于配置，实际借用数据校验在资源服务）
        // 一期先校验是否存在超期90天借用
        // 详细校验在 ResourceService 中，这里预留钩子
    }

    @Override
    public void saveDraft(ApplicationSubmitRequest req) {
        Long userId = UserContext.getUserId();
        TestApplication app;
        if (req.getId() != null) {
            app = applicationMapper.selectById(req.getId());
            if (app == null) throw new BizException("申请单不存在");
            BeanUtils.copyProperties(req, app);
            applicationMapper.updateById(app);
        } else {
            app = new TestApplication();
            BeanUtils.copyProperties(req, app);
            app.setAppNo(genAppNo());
            app.setApplicantId(userId);
            app.setApplicantName(UserContext.getRealName());
            app.setStatus(ST_DRAFT);
            app.setCurrentNode(NODE_PRESALES);
            applicationMapper.insert(app);
        }
    }

    @Override
    @Transactional
    public void approve(ApprovalRequest req) {
        TestApplication app = applicationMapper.selectById(req.getAppId());
        if (app == null) throw new BizException("申请单不存在");
        Long userId = UserContext.getUserId();
        String userName = UserContext.getRealName();
        String node = app.getCurrentNode();

        if ("REJECT".equals(req.getAction())) {
            app.setStatus(ST_REJECTED);
            app.setRejectReason(StringUtils.hasText(req.getRejectReason()) ? req.getRejectReason() : req.getOpinion());
            applicationMapper.updateById(app);
            record(app.getId(), node, userId, userName, "REJECT", req.getOpinion());
            // 通知申请人 -> 跳转测试申请列表
            notifyService.send(app.getApplicantId(), "测试申请被驳回",
                    "您的测试申请【" + app.getProjectName() + "】被驳回，原因：" + app.getRejectReason(),
                    "APPROVAL", app.getId(), "/application");
            return;
        }

        // APPROVE 流程推进
        switch (node) {
            case NODE_APPROVAL -> {
                // 测试审批组通过，判断是否超90天
                int approveDays = Integer.parseInt(dictService.getConfig("loan.approve.days", "90"));
                boolean needLeader = app.getApplyDays() != null && app.getApplyDays() > approveDays;
                if (needLeader) {
                    app.setStatus(ST_LEADER);
                    app.setCurrentNode(NODE_LEADER);
                    applicationMapper.updateById(app);
                    record(app.getId(), node, userId, userName, "APPROVE", req.getOpinion());
                    notifyService.sendToRole("LEADER", "超期借测申请待审批",
                            "测试申请【" + app.getProjectName() + "】申请周期" + app.getApplyDays() + "天，超过" + approveDays + "天，需您审批。",
                            "APPROVAL", app.getId(), "/approval");
                } else {
                    toAssign(app, node, userId, userName, req.getOpinion());
                }
            }
            case NODE_LEADER -> {
                // 魏总审批通过 -> 分配任务
                toAssign(app, node, userId, userName, req.getOpinion() + "(超期借测，需SPM申请)");
            }
            default -> throw new BizException("当前节点不支持审批操作");
        }
    }

    private void toAssign(TestApplication app, String node, Long userId, String userName, String opinion) {
        app.setStatus(ST_ASSIGN);
        app.setCurrentNode(NODE_ASSIGN);
        applicationMapper.updateById(app);
        record(app.getId(), node, userId, userName, "APPROVE", opinion);
        // 流转给FAE测试工程师组负责人(daihw)分配任务 -> 跳转审批中心
        Long assigneeId = getFaeGroupLeaderId();
        if (assigneeId != null) {
            notifyService.send(assigneeId, "测试申请待分配任务",
                    "测试申请【" + app.getProjectName() + "】已审批通过，请分配FAE测试工程师。",
                    "APPROVAL", app.getId(), "/approval");
        } else {
            // 兜底：通知资源管理员角色
            notifyService.sendToRole("RESOURCE_ADMIN", "测试申请待分配资源",
                    "测试申请【" + app.getProjectName() + "】已审批通过，请分配测试资源与人员。",
                    "APPROVAL", app.getId(), "/approval");
        }
    }

    /**
     * 获取FAE测试工程师组负责人id(任务分配人)
     */
    private Long getFaeGroupLeaderId() {
        try {
            com.sugon.testplatform.entity.SysUserGroup group = userGroupMapper.selectOne(
                    new LambdaQueryWrapper<com.sugon.testplatform.entity.SysUserGroup>()
                            .eq(com.sugon.testplatform.entity.SysUserGroup::getGroupCode, "FAE_GROUP"));
            return group == null ? null : group.getLeaderId();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @Transactional
    public void assign(AssignRequest req) {
        TestApplication app = applicationMapper.selectById(req.getAppId());
        if (app == null) throw new BizException("申请单不存在");
        if (!NODE_ASSIGN.equals(app.getCurrentNode())) {
            throw new BizException("当前申请不在待分配节点");
        }
        Long userId = UserContext.getUserId();
        app.setStatus(ST_ASSIGNED);
        app.setCurrentNode(NODE_DONE);
        applicationMapper.updateById(app);

        // 创建项目
        TestProject project = new TestProject();
        BeanUtils.copyProperties(app, project);
        project.setId(null);
        project.setProjectNo("PJ" + app.getAppNo().substring(2));
        // 销售/售前用申请单上选择的关联人
        project.setSalesId(app.getSalesId());
        project.setSalesName(app.getSalesName());
        project.setPresalesId(app.getPresalesId());
        project.setPresalesName(app.getPresalesName());
        project.setTesterIds(req.getTesterIds());
        project.setTesterNames(resolveTesterNames(req.getTesterIds()));
        project.setStatus("NOT_START");
        project.setApplyTime(app.getCreateTime());
        project.setCreateBy(userId);
        projectMapper.insert(project);

        // 回写项目id
        app.setProjectId(project.getId());
        applicationMapper.updateById(app);

        record(app.getId(), NODE_ASSIGN, userId, UserContext.getRealName(), "ASSIGN",
                "分配资源类型:" + req.getResourceType() + ", 测试人员:" + req.getTesterIds());

        // 通知测试人员 -> 跳转项目详情
        if (StringUtils.hasText(req.getTesterIds())) {
            for (String tid : req.getTesterIds().split(",")) {
                try {
                    notifyService.send(Long.valueOf(tid.trim()), "新测试任务分配",
                            "您被分配了测试项目【" + app.getProjectName() + "】，请及时开展测试并填写进展。",
                            "APPROVAL", project.getId(), "/project/" + project.getId());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        // 通知申请人 -> 跳转项目详情
        notifyService.send(app.getApplicantId(), "测试申请已分配",
                "您的测试申请【" + app.getProjectName() + "】已分配资源与测试人员，项目编号:" + project.getProjectNo(),
                "APPROVAL", project.getId(), "/project/" + project.getId());
    }

    @Override
    @Transactional
    public void withdraw(Long appId) {
        TestApplication app = applicationMapper.selectById(appId);
        if (app == null) throw new BizException("申请单不存在");
        if (!app.getApplicantId().equals(UserContext.getUserId())) {
            throw new BizException("只能撤回自己的申请");
        }
        app.setStatus(ST_CLOSED);
        applicationMapper.updateById(app);
        record(appId, app.getCurrentNode(), UserContext.getUserId(), UserContext.getRealName(), "WITHDRAW", "申请人撤回");
    }

    @Override
    public PageResult<TestApplication> page(int pageNum, int pageSize, String status, String keyword, Long applicantId, boolean todoOnly) {
        Page<TestApplication> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TestApplication> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) qw.eq(TestApplication::getStatus, status);
        if (applicantId != null) qw.eq(TestApplication::getApplicantId, applicantId);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(TestApplication::getProjectName, keyword)
                    .or().like(TestApplication::getCustomerName, keyword)
                    .or().like(TestApplication::getAppNo, keyword));
        }
        // 数据权限：审批组/领导/管理员看全部；其他只看自己申请或关联自己的
        applyAppDataScope(qw);
        qw.orderByDesc(TestApplication::getCreateTime);
        Page<TestApplication> result = applicationMapper.selectPage(page, qw);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    /**
     * 申请单数据权限
     * 审批组(APPROVER)/领导(LEADER)/管理员(ADMIN)/资源管理员: 全部
     * 售前(PRESALES): 自己提交的(applicant_id) 或 作为关联售前(presales_id)
     * 销售(SALES): 关联销售是自己的(sales_id) 或 自己提交的
     * 其他: 仅自己提交的
     */
    private void applyAppDataScope(LambdaQueryWrapper<TestApplication> qw) {
        java.util.List<String> roles = UserContext.getRoles();
        Long uid = UserContext.getUserId();
        if (uid == null) {
            qw.eq(TestApplication::getId, -1); // 未登录看不到任何数据
            return;
        }
        boolean seeAll = roles.contains("ADMIN") || roles.contains("APPROVER")
                || roles.contains("LEADER") || roles.contains("RESOURCE_ADMIN")
                || roles.contains("FAE_LEADER");
        if (seeAll) return;

        if (roles.contains("PRESALES")) {
            qw.and(w -> w.eq(TestApplication::getApplicantId, uid)
                    .or().eq(TestApplication::getPresalesId, uid));
        } else if (roles.contains("SALES")) {
            qw.and(w -> w.eq(TestApplication::getSalesId, uid)
                    .or().eq(TestApplication::getApplicantId, uid));
        } else {
            qw.eq(TestApplication::getApplicantId, uid);
        }
    }

    @Override
    public TestApplication detail(Long id) {
        return applicationMapper.selectById(id);
    }

    @Override
    public PageResult<TestApplication> todoList(Long userId, int pageNum, int pageSize) {
        // 根据用户角色返回待办：多角色用户的待办是各角色节点待办的并集
        List<String> roles = UserContext.getRoles();
        Page<TestApplication> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TestApplication> qw = new LambdaQueryWrapper<>();

        // 收集该用户所有角色对应的待办节点
        List<String> nodes = new java.util.ArrayList<>();
        if (roles.contains("PRESALES")) nodes.add(NODE_PRESALES);
        if (roles.contains("APPROVER")) nodes.add(NODE_APPROVAL);
        if (roles.contains("LEADER")) nodes.add(NODE_LEADER);
        if (roles.contains("RESOURCE_ADMIN")) nodes.add(NODE_ASSIGN);

        if (!nodes.isEmpty()) {
            qw.in(TestApplication::getCurrentNode, nodes);
        } else {
            // 无流程角色的普通用户，待办=自己提交且未完成的申请
            qw.eq(TestApplication::getApplicantId, userId);
        }
        qw.orderByDesc(TestApplication::getCreateTime);
        Page<TestApplication> result = applicationMapper.selectPage(page, qw);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    private void record(Long appId, String node, Long approverId, String approverName, String action, String opinion) {
        ApprovalRecord r = new ApprovalRecord();
        r.setAppId(appId);
        r.setNode(node);
        r.setApproverId(approverId);
        r.setApproverName(approverName);
        r.setAction(action);
        r.setOpinion(opinion);
        approvalRecordMapper.insert(r);
    }

    /**
     * 根据逗号分隔的测试人员id解析姓名
     */
    private String resolveTesterNames(String testerIds) {
        if (!StringUtils.hasText(testerIds)) return null;
        List<Long> ids = java.util.Arrays.stream(testerIds.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(s -> {
                    try { return Long.valueOf(s); } catch (NumberFormatException e) { return null; }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        if (ids.isEmpty()) return null;
        return userMapper.selectBatchIds(ids).stream()
                .map(com.sugon.testplatform.entity.SysUser::getRealName)
                .collect(Collectors.joining("/"));
    }

    /**
     * 测试类型规范化: 把 '["AI","CPU"]' JSON数组字符串 转为 'AI、CPU' 纯文本
     */
    private String normalizeTestType(String testType) {
        if (!StringUtils.hasText(testType)) return testType;
        String t = testType.trim();
        if (t.startsWith("[") && t.endsWith("]")) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                List<String> arr = om.readValue(t,
                        om.getTypeFactory().constructCollectionType(List.class, String.class));
                return String.join("、", arr);
            } catch (Exception e) {
                // 解析失败则去括号引号兜底
                return t.replace("[", "").replace("]", "").replace("\"", "").replace(",", "、");
            }
        }
        return t;
    }
}
