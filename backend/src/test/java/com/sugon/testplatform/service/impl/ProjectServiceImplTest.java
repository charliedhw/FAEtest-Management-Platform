package com.sugon.testplatform.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.sugon.testplatform.common.BizException;
import com.sugon.testplatform.entity.TestProject;
import com.sugon.testplatform.entity.TestReport;
import com.sugon.testplatform.mapper.SysUserMapper;
import com.sugon.testplatform.mapper.TestApplicationMapper;
import com.sugon.testplatform.mapper.TestProgressMapper;
import com.sugon.testplatform.mapper.TestProjectMapper;
import com.sugon.testplatform.mapper.TestReportMapper;
import com.sugon.testplatform.mapper.TestStageMapper;
import com.sugon.testplatform.security.LoginUser;
import com.sugon.testplatform.security.PresalesScope;
import com.sugon.testplatform.security.UserContext;
import com.sugon.testplatform.service.ReportService;
import com.sugon.testplatform.service.ResourceService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProjectServiceImplTest {
    private final TestProjectMapper projectMapper = mock(TestProjectMapper.class);
    private final TestReportMapper reportMapper = mock(TestReportMapper.class);
    private final ResourceService resourceService = mock(ResourceService.class);
    private final ProjectServiceImpl service = new ProjectServiceImpl(
            projectMapper, mock(TestProgressMapper.class), reportMapper, mock(TestApplicationMapper.class),
            mock(ReportService.class), resourceService, mock(SysUserMapper.class),
            mock(PresalesScope.class), mock(TestStageMapper.class));
    private TestProject project;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TestReport.class);
        LoginUser user = new LoginUser();
        user.setUserId(7L);
        user.setRoles(List.of("TESTER"));
        UserContext.set(user);
        project = new TestProject();
        project.setId(1L);
        project.setTesterIds("7,8");
        project.setStatus("IN_PROGRESS");
        project.setTestConclusion("测试通过");
        when(projectMapper.selectById(1L)).thenReturn(project);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void completionRejectsBlankConclusion() {
        project.setTestConclusion("  \n");
        BizException error = assertThrows(BizException.class, () -> service.updateStatus(1L, "COMPLETED"));
        assertEquals("完成测试项目前必须填写测试结论", error.getMessage());
        verify(reportMapper, never()).selectCount(any());
        verify(projectMapper, never()).updateById(any(TestProject.class));
        verifyNoInteractions(resourceService);
    }

    @Test
    void completionRejectsMissingReport() {
        when(reportMapper.selectCount(any())).thenReturn(0L);
        BizException error = assertThrows(BizException.class, () -> service.updateStatus(1L, "COMPLETED"));
        assertEquals("完成测试项目前必须上传至少一份测试报告", error.getMessage());
        verify(projectMapper, never()).updateById(any(TestProject.class));
        verifyNoInteractions(resourceService);
    }

    @Test
    void completionSucceedsWithConclusionAndReport() {
        when(reportMapper.selectCount(any())).thenReturn(1L);
        when(resourceService.recycleByProject(1L)).thenReturn(2);
        service.updateStatus(1L, "COMPLETED");
        ArgumentCaptor<TestProject> updated = ArgumentCaptor.forClass(TestProject.class);
        verify(projectMapper).updateById(updated.capture());
        assertEquals(1L, updated.getValue().getId());
        assertEquals("COMPLETED", updated.getValue().getStatus());
        verify(resourceService).recycleByProject(1L);
    }

    @Test
    void genericUpdateCannotBypassCompletionRequirements() {
        project.setTestConclusion(null);
        TestProject update = new TestProject();
        update.setId(1L);
        update.setStatus("COMPLETED");
        BizException error = assertThrows(BizException.class, () -> service.update(update));
        assertEquals("完成测试项目前必须填写测试结论", error.getMessage());
        verify(projectMapper, never()).updateById(any(TestProject.class));
    }

    @Test
    void genericUpdateUsesSubmittedConclusionForCompletionCheck() {
        project.setTestConclusion(null);
        when(reportMapper.selectCount(any())).thenReturn(1L);
        TestProject update = new TestProject();
        update.setId(1L);
        update.setStatus("COMPLETED");
        update.setTestConclusion("复核后通过");
        service.update(update);
        verify(projectMapper).updateById(update);
    }

    @Test
    void otherStatusChangesKeepExistingBehavior() {
        project.setTestConclusion(null);
        service.updateStatus(1L, "PAUSED");
        verify(reportMapper, never()).selectCount(any());
        verify(projectMapper).updateById(any(TestProject.class));
        verifyNoInteractions(resourceService);
    }
}
