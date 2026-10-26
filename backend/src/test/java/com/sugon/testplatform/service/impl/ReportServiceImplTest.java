package com.sugon.testplatform.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sugon.testplatform.common.BizException;
import com.sugon.testplatform.common.PageResult;
import com.sugon.testplatform.dto.TestReportView;
import com.sugon.testplatform.entity.TestProject;
import com.sugon.testplatform.entity.TestReport;
import com.sugon.testplatform.mapper.TestProjectMapper;
import com.sugon.testplatform.mapper.TestReportMapper;
import com.sugon.testplatform.security.LoginUser;
import com.sugon.testplatform.security.UserContext;
import com.sugon.testplatform.service.ProjectService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReportServiceImplTest {
    private final TestReportMapper reportMapper = mock(TestReportMapper.class);
    private final TestProjectMapper projectMapper = mock(TestProjectMapper.class);
    private final ProjectService projectService = mock(ProjectService.class);
    private final ReportServiceImpl service = new ReportServiceImpl(reportMapper, projectMapper);

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TestReport.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TestProject.class);
        ReflectionTestUtils.setField(service, "projectService", projectService);
        setRoles("APPROVER");
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void testerReportPageCombinesFiltersWithoutProjectScope() {
        setRoles("TESTER");
        TestReport report = new TestReport();
        report.setId(11L);
        report.setProjectId(1L);
        report.setFileName("测试报告.pdf");
        report.setFileKey("private-key");
        report.setFileSize(2048L);
        report.setVersion(2);
        report.setUploadByName("工程师");
        report.setCreateTime(LocalDateTime.of(2026, 10, 26, 10, 0));
        when(reportMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            LambdaQueryWrapper<TestReport> query = invocation.getArgument(1);
            String sql = query.getSqlSegment();
            assertTrue(sql.contains("EXISTS"));
            assertTrue(sql.contains("FROM test_project WHERE"));
            assertTrue(sql.contains("test_report.project_id"));
            assertTrue(sql.contains("tester_ids"));
            assertTrue(sql.contains("FIND_IN_SET"));
            assertTrue(sql.contains("test_type"));
            assertTrue(sql.contains("device_type"));
            assertTrue(sql.contains("project_name"));
            assertFalse(sql.contains("create_by"));
            assertTrue(query.getParamNameValuePairs().values().containsAll(
                    List.of("7", "AI", "BW1000", "%客户%")));
            Page<TestReport> page = invocation.getArgument(0);
            page.setTotal(1);
            page.setRecords(List.of(report));
            return page;
        });
        TestProject project = new TestProject();
        project.setId(1L);
        project.setProjectNo("TP001");
        project.setProjectName("性能验证");
        project.setCustomerName("客户A");
        project.setTesterNames("工程师");
        project.setTestType("AI、CPU");
        project.setDeviceType("BW1000");
        project.setStatus("COMPLETED");
        when(projectMapper.selectBatchIds(any())).thenReturn(List.of(project));

        PageResult<TestReportView> result = service.page(1, 10,
                Map.of("testerId", 7L, "testType", "AI", "deviceType", "BW1000", "keyword", "客户"));

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        TestReportView view = result.getRecords().get(0);
        assertEquals(11L, view.getId());
        assertEquals("测试报告.pdf", view.getFileName());
        assertEquals("TP001", view.getProjectNo());
        assertEquals("性能验证", view.getProjectName());
        assertEquals("AI、CPU", view.getTestType());
        assertEquals("COMPLETED", view.getProjectStatus());
        verify(projectService, never()).applyDataScope(ArgumentMatchers.<LambdaQueryWrapper<TestProject>>any());
    }

    @Test
    void emptyPagePreservesTotalAndSkipsProjectLookup() {
        when(reportMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<TestReport> page = invocation.getArgument(0);
            page.setTotal(23);
            page.setRecords(List.of());
            return page;
        });
        PageResult<TestReportView> result = service.page(3, 10, null);
        assertEquals(23L, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
        verify(projectMapper, never()).selectBatchIds(any());
    }

    @Test
    void rejectsRolesOutsideReportCenter() {
        setRoles("PRESALES");
        BizException error = assertThrows(BizException.class, () -> service.page(1, 10, null));
        assertTrue(error.getMessage().contains("仅售前审批组"));
        verifyNoInteractions(reportMapper, projectMapper, projectService);
    }

    @Test
    void rejectsInvalidPaginationBeforeQuerying() {
        assertThrows(BizException.class, () -> service.page(0, 10, null));
        assertThrows(BizException.class, () -> service.page(1, 0, null));
        assertThrows(BizException.class, () -> service.page(1, 201, null));
        verifyNoInteractions(reportMapper, projectMapper, projectService);
    }

    @Test
    void batchDownloadTesterDoesNotCheckProjectScope() {
        setRoles("TESTER");
        TestReport report = new TestReport();
        report.setId(11L);
        report.setProjectId(1L);
        report.setFileKey("private-key");
        when(reportMapper.selectBatchIds(any())).thenReturn(List.of(report));
        when(projectMapper.selectBatchIds(any())).thenReturn(List.of());
        BizException error = assertThrows(BizException.class,
                () -> service.batchDownload(List.of(11L), mock(HttpServletResponse.class)));
        assertTrue(error.getMessage().contains("文件存储服务不可用"));
        verify(projectService, never()).canView(any());
    }

    @Test
    void singleDownloadTesterDoesNotCheckProjectScope() {
        setRoles("TESTER");
        TestReport report = new TestReport();
        report.setId(11L);
        report.setProjectId(1L);
        report.setFileKey("private-key");
        when(reportMapper.selectById(11L)).thenReturn(report);
        BizException error = assertThrows(BizException.class, () -> service.download(11L));
        assertTrue(error.getMessage().contains("文件存储服务不可用"));
        verify(projectService, never()).canView(any());
    }

    @Test
    void batchDownloadValidatesSelectionAndBatchSize() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        assertThrows(BizException.class, () -> service.batchDownload(null, response));
        assertThrows(BizException.class, () -> service.batchDownload(List.of(), response));
        assertThrows(BizException.class,
                () -> service.batchDownload(LongStream.rangeClosed(1, 101).boxed().toList(), response));
        verifyNoInteractions(reportMapper, projectMapper, projectService);
    }

    private void setRoles(String... roles) {
        LoginUser user = new LoginUser();
        user.setUserId(7L);
        user.setRoles(List.of(roles));
        UserContext.set(user);
    }
}
