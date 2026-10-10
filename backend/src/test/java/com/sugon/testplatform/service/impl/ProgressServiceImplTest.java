package com.sugon.testplatform.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.sugon.testplatform.common.BizException;
import com.sugon.testplatform.common.PageResult;
import com.sugon.testplatform.entity.TestProgress;
import com.sugon.testplatform.entity.TestProject;
import com.sugon.testplatform.mapper.TestProgressMapper;
import com.sugon.testplatform.mapper.TestProjectMapper;
import com.sugon.testplatform.mapper.TestStageMapper;
import com.sugon.testplatform.service.ProjectService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProgressServiceImplTest {
    private final TestProgressMapper progressMapper = mock(TestProgressMapper.class);
    private final TestProjectMapper projectMapper = mock(TestProjectMapper.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final ProgressServiceImpl service = new ProgressServiceImpl(
            progressMapper, projectMapper, projectService, mock(TestStageMapper.class));
    private final LocalDate date = LocalDate.of(2026, 10, 10);

    @BeforeEach
    void initializeMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                TestProgress.class);
    }

    @Test
    void includesUnreportedProjectsAndAllDailyEntriesWithinVisibleProjectPage() {
        TestProject reported = project(1L);
        TestProject missing = project(2L);
        when(projectService.page(2, 20, Map.of("status", "IN_PROGRESS", "keyword", "客户")))
                .thenReturn(new PageResult<>(25L, List.of(reported, missing)));
        TestProgress latest = progress(11L, 1L, "当日第二条");
        TestProgress earlier = progress(10L, 1L, "当日第一条");
        when(progressMapper.selectList(any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<TestProgress> query = invocation.getArgument(0);
            String sql = query.getSqlSegment();
            assertTrue(sql.contains("project_id IN"));
            assertTrue(sql.contains("progress_date ="));
            assertTrue(sql.contains("ORDER BY create_time DESC,id DESC"));
            assertTrue(query.getParamNameValuePairs().containsValue(date));
            assertTrue(query.getParamNameValuePairs().containsValue(1L));
            assertTrue(query.getParamNameValuePairs().containsValue(2L));
            assertEquals(3, query.getParamNameValuePairs().size());
            return List.of(latest, earlier);
        });

        PageResult<Map<String, Object>> result = service.projectDaily(2, 20, "客户", date);
        assertEquals(25L, result.getTotal());
        assertEquals(2, result.getRecords().size());
        Map<String, Object> filled = result.getRecords().get(0);
        assertEquals(1L, filled.get("projectId"));
        assertEquals(2, filled.get("progressCount"));
        assertEquals("当日第二条", filled.get("latestProgressContent"));
        assertEquals(date, filled.get("latestProgressDate"));
        assertEquals(List.of(latest, earlier), filled.get("dailyProgress"));
        Map<String, Object> unfilled = result.getRecords().get(1);
        assertEquals(2L, unfilled.get("projectId"));
        assertEquals(0, unfilled.get("progressCount"));
        assertEquals(List.of(), unfilled.get("dailyProgress"));
        assertFalse(unfilled.containsKey("latestProgressContent"));
        verifyNoInteractions(projectMapper);
    }

    @Test
    void keepsProjectsWhenNoDailyProgressExists() {
        when(projectService.page(anyInt(), anyInt(), any()))
                .thenReturn(new PageResult<>(1L, List.of(project(1L))));
        when(progressMapper.selectList(any())).thenReturn(List.of());
        PageResult<Map<String, Object>> result = service.projectDaily(1, 20, null, date);
        assertEquals(1L, result.getTotal());
        assertEquals(0, result.getRecords().get(0).get("progressCount"));
    }

    @Test
    void emptyOrOutOfRangePageDoesNotQueryProgressAndPreservesTotal() {
        when(projectService.page(anyInt(), anyInt(), any()))
                .thenReturn(new PageResult<>(30L, List.of()));
        PageResult<Map<String, Object>> result = service.projectDaily(3, 20, "", date);
        assertEquals(30L, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
        verifyNoInteractions(progressMapper);
    }

    @Test
    void rejectsInvalidPaginationAndMissingDate() {
        assertThrows(BizException.class, () -> service.projectDaily(0, 20, null, date));
        assertThrows(BizException.class, () -> service.projectDaily(1, 0, null, date));
        assertThrows(BizException.class, () -> service.projectDaily(1, 201, null, date));
        assertThrows(BizException.class, () -> service.projectDaily(1, 20, null, null));
        verifyNoInteractions(projectService, progressMapper);
    }

    @Test
    void timeSummaryStillIncludesOnlyProjectsWithProgress() {
        TestProgress progress = progress(1L, 1L, "当日进度");
        when(progressMapper.selectList(any())).thenReturn(List.of(progress));
        when(projectMapper.selectList(any())).thenReturn(List.of(project(1L), project(2L)));
        PageResult<Map<String, Object>> result = service.projectSummary(1, 20, "", date.toString());
        assertEquals(1L, result.getTotal());
        assertEquals(1L, result.getRecords().get(0).get("projectId"));
        assertEquals("当日进度", result.getRecords().get(0).get("latestProgressContent"));
    }

    private TestProject project(Long id) {
        TestProject project = new TestProject();
        project.setId(id);
        project.setProjectName("项目" + id);
        project.setStatus("IN_PROGRESS");
        return project;
    }

    private TestProgress progress(Long id, Long projectId, String content) {
        TestProgress progress = new TestProgress();
        progress.setId(id);
        progress.setProjectId(projectId);
        progress.setProgressDate(date);
        progress.setContent(content);
        progress.setCreateByName("测试人员");
        return progress;
    }
}
