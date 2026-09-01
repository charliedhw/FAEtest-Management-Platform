package com.sugon.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sugon.testplatform.common.BizException;
import com.sugon.testplatform.entity.TestProgress;
import com.sugon.testplatform.entity.TestStage;
import com.sugon.testplatform.mapper.TestProgressMapper;
import com.sugon.testplatform.mapper.TestStageMapper;
import com.sugon.testplatform.security.UserContext;
import com.sugon.testplatform.service.ProjectService;
import com.sugon.testplatform.service.StageService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StageServiceImpl implements StageService {
    private final TestStageMapper stageMapper;
    private final TestProgressMapper progressMapper;
    private final ProjectService projectService;

    public StageServiceImpl(TestStageMapper stageMapper, TestProgressMapper progressMapper,
                            @Lazy ProjectService projectService) {
        this.stageMapper = stageMapper;
        this.progressMapper = progressMapper;
        this.projectService = projectService;
    }

    @Override
    public TestStage add(TestStage stage) {
        // 只有被分配的FAE测试人员或管理员能拆分阶段任务
        if (!projectService.canEditProgress(stage.getProjectId())) {
            throw new BizException("只有接受测试任务的FAE测试人员或管理员才能拆分阶段任务");
        }
        stage.setCreateBy(UserContext.getUserId());
        stage.setCreateByName(UserContext.getRealName());
        if (stage.getStatus() == null) stage.setStatus("NOT_START");
        stageMapper.insert(stage);
        return stage;
    }

    @Override
    public void update(TestStage stage) {
        if (!projectService.canEditProgress(stage.getProjectId())) {
            throw new BizException("无权限修改该项目的阶段任务");
        }
        stageMapper.updateById(stage);
    }

    @Override
    public void delete(Long id) {
        TestStage stage = stageMapper.selectById(id);
        if (stage != null && !projectService.canEditProgress(stage.getProjectId())) {
            throw new BizException("无权限删除该阶段任务");
        }
        stageMapper.deleteById(id);
    }

    @Override
    public List<TestStage> listByProject(Long projectId) {
        List<TestStage> stages = stageMapper.selectList(new LambdaQueryWrapper<TestStage>()
                .eq(TestStage::getProjectId, projectId)
                .orderByAsc(TestStage::getSort)
                .orderByAsc(TestStage::getPlanStart));
        // 统计每个阶段的日志数
        for (TestStage s : stages) {
            Long cnt = progressMapper.selectCount(new LambdaQueryWrapper<TestProgress>()
                    .eq(TestProgress::getProjectId, projectId)
                    .eq(TestProgress::getStageId, s.getId()));
            s.setProgressCount(cnt.intValue());
        }
        return stages;
    }

    @Override
    public Map<String, Object> projectProgress(Long projectId) {
        List<TestStage> stages = listByProject(projectId);
        int total = stages.size();
        int done = 0, inProgress = 0, notStart = 0;
        for (TestStage s : stages) {
            if ("DONE".equals(s.getStatus())) done++;
            else if ("IN_PROGRESS".equals(s.getStatus())) inProgress++;
            else notStart++;
        }
        int percent = total == 0 ? 0 : Math.round(done * 100.0f / total);
        Map<String, Object> result = new HashMap<>();
        result.put("stages", stages);
        result.put("total", total);
        result.put("done", done);
        result.put("inProgress", inProgress);
        result.put("notStart", notStart);
        result.put("percent", percent);
        return result;
    }
}
