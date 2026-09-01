package com.sugon.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sugon.testplatform.common.BizException;
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
        progressMapper.updateById(progress);
    }

    @Override
    public void delete(Long id) {
        progressMapper.deleteById(id);
    }

    @Override
    public List<TestProgress> listByProject(Long projectId) {
        return progressMapper.selectList(new LambdaQueryWrapper<TestProgress>()
                .eq(TestProgress::getProjectId, projectId)
                .orderByDesc(TestProgress::getProgressDate)
                .orderByDesc(TestProgress::getCreateTime));
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

            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("projectId", proj.getId());
            map.put("projectNo", proj.getProjectNo());
            map.put("projectName", proj.getProjectName());
            map.put("customerName", proj.getCustomerName());
            map.put("status", proj.getStatus());
            map.put("testerNames", proj.getTesterNames());
            map.put("testStartTime", proj.getTestStartTime());
            map.put("testEndTime", proj.getTestEndTime());

            // 最新进展
            TestProgress latest = projProgress.get(0);
            map.put("latestProgressDate", latest.getProgressDate());
            map.put("latestProgressContent", latest.getContent());
            map.put("latestProgressBy", latest.getCreateByName());
            // 当日/全部进展数
            map.put("progressCount", projProgress.size());

            allList.add(map);
        }

        // 手动分页
        int total = allList.size();
        int from = (pageNum - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        List<java.util.Map<String, Object>> paged = from < total ? allList.subList(from, to) : java.util.Collections.emptyList();
        return new com.sugon.testplatform.common.PageResult<>((long) total, paged);
    }
}
