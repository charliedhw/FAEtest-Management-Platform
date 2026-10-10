package com.sugon.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sugon.testplatform.common.BizException;
import com.sugon.testplatform.common.PageResult;
import com.sugon.testplatform.dto.ProgressRequest;
import com.sugon.testplatform.entity.TestProgress;
import com.sugon.testplatform.entity.TestProject;
import com.sugon.testplatform.mapper.TestProgressMapper;
import com.sugon.testplatform.mapper.TestProjectMapper;
import com.sugon.testplatform.mapper.TestStageMapper;
import com.sugon.testplatform.security.UserContext;
import com.sugon.testplatform.service.ProgressService;
import com.sugon.testplatform.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProgressServiceImpl implements ProgressService {
    private final TestProgressMapper progressMapper;
    private final TestProjectMapper projectMapper;
    private final ProjectService projectService;
    private final TestStageMapper stageMapper;

    public ProgressServiceImpl(TestProgressMapper progressMapper, TestProjectMapper projectMapper,
                               @Lazy ProjectService projectService, TestStageMapper stageMapper) {
        this.progressMapper = progressMapper;
        this.projectMapper = projectMapper;
        this.projectService = projectService;
        this.stageMapper = stageMapper;
    }

    @Override
    public void add(ProgressRequest req) {
        // 只有被分配的FAE测试人员及管理员才能填写进展
        if (!projectService.canEditProgress(req.getProjectId())) {
            throw new BizException("只有接受测试任务的FAE测试人员或管理员才能填写进展");
        }
        TestProgress p = new TestProgress();
        p.setProjectId(req.getProjectId());
        p.setStageId(req.getStageId());
        p.setProgressDate(req.getProgressDate() == null ? LocalDate.now() : req.getProgressDate());
        p.setContent(req.getContent());
        p.setCreateBy(UserContext.getUserId());
        p.setCreateByName(UserContext.getRealName());
        progressMapper.insert(p);

        // 如果关联了阶段,把该阶段标记为进行中
        if (req.getStageId() != null) {
            com.sugon.testplatform.entity.TestStage stage = new com.sugon.testplatform.entity.TestStage();
            stage.setId(req.getStageId());
            stage.setStatus("IN_PROGRESS");
            stageMapper.updateById(stage);
        }

        // 更新项目状态为进行中 + 更新时间
        TestProject update = new TestProject();
        update.setId(req.getProjectId());
        update.setStatus("IN_PROGRESS");
        projectMapper.updateById(update);
    }

    @Override
    public void update(TestProgress progress) {
        TestProgress old = progressMapper.selectById(progress.getId());
        if (old == null) throw new BizException("进展不存在");
        if (!projectService.canEditProgress(old.getProjectId())) {
            throw new BizException("无权修改该项目的进展");
        }
        progress.setProjectId(old.getProjectId());
        progressMapper.updateById(progress);
    }

    @Override
    public void delete(Long id) {
        TestProgress old = progressMapper.selectById(id);
        if (old == null) return;
        if (!projectService.canEditProgress(old.getProjectId())) {
            throw new BizException("无权删除该项目的进展");
        }
        progressMapper.deleteById(id);
    }

    @Override
    public List<TestProgress> listByProject(Long projectId) {
        if (!projectService.canView(projectId)) {
            throw new BizException("无权查看该项目的进展");
        }
        return progressMapper.selectList(new LambdaQueryWrapper<TestProgress>()
                .eq(TestProgress::getProjectId, projectId)
                .orderByDesc(TestProgress::getProgressDate)
                .orderByDesc(TestProgress::getCreateTime));
    }

    @Override
    public PageResult<Map<String, Object>> projectDaily(int pageNum, int pageSize, String keyword, LocalDate date) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 200) {
            throw new BizException("分页参数无效，每页条数应为1至200");
        }
        if (date == null) throw new BizException("请选择日报日期");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("status", "IN_PROGRESS");
        params.put("keyword", keyword);
        // 复用项目清单的数据范围（包含售前区域组），只查询当前页项目的日报。
        PageResult<TestProject> page = projectService.page(pageNum, pageSize, params);
        if (page.getRecords().isEmpty()) {
            return new PageResult<>(page.getTotal(), Collections.emptyList());
        }
        List<Long> ids = page.getRecords().stream().map(TestProject::getId).collect(Collectors.toList());
        List<TestProgress> progress = progressMapper.selectList(new LambdaQueryWrapper<TestProgress>()
                .in(TestProgress::getProjectId, ids)
                .eq(TestProgress::getProgressDate, date)
                .orderByDesc(TestProgress::getCreateTime)
                .orderByDesc(TestProgress::getId));
        Map<Long, List<TestProgress>> grouped = progress.stream()
                .collect(Collectors.groupingBy(TestProgress::getProjectId));
        List<Map<String, Object>> records = new ArrayList<>();
        for (TestProject project : page.getRecords()) {
            List<TestProgress> daily = grouped.getOrDefault(project.getId(), Collections.emptyList());
            Map<String, Object> row = summaryRow(project, daily);
            row.put("dailyProgress", daily);
            records.add(row);
        }
        return new PageResult<>(page.getTotal(), records);
    }

    private Map<String, Object> summaryRow(TestProject project, List<TestProgress> progress) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("projectId", project.getId());
        row.put("projectNo", project.getProjectNo());
        row.put("projectName", project.getProjectName());
        row.put("customerName", project.getCustomerName());
        row.put("status", project.getStatus());
        row.put("testerNames", project.getTesterNames());
        row.put("testStartTime", project.getTestStartTime());
        row.put("testEndTime", project.getTestEndTime());
        if (!progress.isEmpty()) {
            TestProgress latest = progress.get(0);
            row.put("latestProgressDate", latest.getProgressDate());
            row.put("latestProgressContent", latest.getContent());
            row.put("latestProgressBy", latest.getCreateByName());
        }
        row.put("progressCount", progress.size());
        return row;
    }

    @Override
    public com.sugon.testplatform.common.PageResult<java.util.Map<String, Object>> projectSummary(int pageNum, int pageSize, String keyword, String date) {
        // 构建进展查询条件
        LambdaQueryWrapper<TestProgress> wrapper = new LambdaQueryWrapper<>();
        if (org.springframework.util.StringUtils.hasText(date)) {
            // 按日期过滤：只查该日期的进展
            wrapper.eq(TestProgress::getProgressDate, java.time.LocalDate.parse(date));
        }
        wrapper.orderByDesc(TestProgress::getProgressDate).orderByDesc(TestProgress::getCreateTime);
        List<TestProgress> progressList = progressMapper.selectList(wrapper);

        if (progressList.isEmpty()) {
            return new com.sugon.testplatform.common.PageResult<>(0L, java.util.Collections.emptyList());
        }

        // 按项目分组
        java.util.Map<Long, List<TestProgress>> grouped = progressList.stream()
                .collect(java.util.stream.Collectors.groupingBy(TestProgress::getProjectId, java.util.LinkedHashMap::new, java.util.stream.Collectors.toList()));

        List<Long> projectIds = new java.util.ArrayList<>(grouped.keySet());

        // 查询项目信息
        LambdaQueryWrapper<TestProject> projWrapper = new LambdaQueryWrapper<>();
        projWrapper.in(TestProject::getId, projectIds);
        if (org.springframework.util.StringUtils.hasText(keyword)) {
            projWrapper.and(w -> w.like(TestProject::getProjectName, keyword)
                    .or().like(TestProject::getProjectNo, keyword)
                    .or().like(TestProject::getCustomerName, keyword));
        }
        projWrapper.ne(TestProject::getStatus, "DELETED");
        List<TestProject> projects = projectMapper.selectList(projWrapper);

        // 组装结果
        List<java.util.Map<String, Object>> allList = new java.util.ArrayList<>();
        for (TestProject proj : projects) {
            List<TestProgress> projProgress = grouped.get(proj.getId());
            if (projProgress == null || projProgress.isEmpty()) continue;

            allList.add(summaryRow(proj, projProgress));
        }

        // 手动分页
        int total = allList.size();
        int from = (pageNum - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        List<java.util.Map<String, Object>> paged = from < total ? allList.subList(from, to) : java.util.Collections.emptyList();
        return new com.sugon.testplatform.common.PageResult<>((long) total, paged);
    }
}
