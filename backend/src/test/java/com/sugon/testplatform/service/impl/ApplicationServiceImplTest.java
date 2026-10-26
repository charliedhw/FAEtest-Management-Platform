package com.sugon.testplatform.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sugon.testplatform.entity.TestApplication;
import com.sugon.testplatform.mapper.ApprovalRecordMapper;
import com.sugon.testplatform.mapper.SysUserGroupMapper;
import com.sugon.testplatform.mapper.SysUserMapper;
import com.sugon.testplatform.mapper.TestApplicationMapper;
import com.sugon.testplatform.mapper.TestProjectMapper;
import com.sugon.testplatform.security.LoginUser;
import com.sugon.testplatform.security.PresalesScope;
import com.sugon.testplatform.security.UserContext;
import com.sugon.testplatform.service.AttachmentService;
import com.sugon.testplatform.service.DictService;
import com.sugon.testplatform.service.MailService;
import com.sugon.testplatform.service.NotifyService;
import com.sugon.testplatform.service.UserService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApplicationServiceImplTest {
    private final TestApplicationMapper applicationMapper = mock(TestApplicationMapper.class);
    private final PresalesScope presalesScope = mock(PresalesScope.class);
    private final ApplicationServiceImpl service = new ApplicationServiceImpl(
            applicationMapper, mock(ApprovalRecordMapper.class), mock(TestProjectMapper.class),
            mock(NotifyService.class), mock(DictService.class), mock(SysUserMapper.class),
            mock(SysUserGroupMapper.class), mock(MailService.class), mock(UserService.class),
            presalesScope, mock(AttachmentService.class));

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TestApplication.class);
        setRoles("ADMIN");
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void normalApplicationViewExcludesAssigned() {
        mockEmptyPage(query -> {
            String sql = query.getSqlSegment();
            assertTrue(sql.contains("status <>") || sql.contains("status !="));
            assertTrue(query.getParamNameValuePairs().containsValue("ASSIGNED"));
        });
        service.page(1, 10, null, null, null, false, false);
    }

    @Test
    void assignedViewOnlyReturnsAssigned() {
        mockEmptyPage(query -> {
            assertTrue(query.getSqlSegment().contains("status ="));
            assertTrue(query.getParamNameValuePairs().containsValue("ASSIGNED"));
        });
        service.page(1, 10, null, null, null, false, true);
    }

    @Test
    void approvedHistoryUsesApprovalRecordsAndKeepsDataScope() {
        setRoles("PRESALES");
        when(presalesScope.memberIdsOfLedGroups(7L)).thenReturn(List.of());
        mockEmptyPage(query -> {
            String sql = query.getSqlSegment();
            assertTrue(sql.contains("EXISTS"));
            assertTrue(sql.contains("approval_record"));
            assertTrue(sql.contains("APPROVE"));
            assertTrue(sql.contains("applicant_id"));
            assertTrue(sql.contains("presales_id"));
            assertTrue(query.getParamNameValuePairs().containsValue(7L));
        });
        service.approvedList(1, 10, null);
    }

    @Test
    void adminTodoIncludesActionableNodesAndExcludesProcessedRecords() {
        mockEmptyPage(query -> {
            String sql = query.getSqlSegment();
            assertTrue(sql.contains("current_node IN"));
            assertTrue(query.getParamNameValuePairs().values().containsAll(List.of(
                    "APPROVAL", "LEADER_APPROVAL", "ASSIGN", "ASSIGNED", "REJECTED", "CLOSED", "DRAFT")));
        });
        service.todoList(7L, 1, 10);
    }

    private void mockEmptyPage(java.util.function.Consumer<LambdaQueryWrapper<TestApplication>> check) {
        when(applicationMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            check.accept(invocation.getArgument(1));
            Page<TestApplication> page = invocation.getArgument(0);
            page.setTotal(0);
            page.setRecords(List.of());
            return page;
        });
    }

    private void setRoles(String... roles) {
        LoginUser user = new LoginUser();
        user.setUserId(7L);
        user.setRoles(List.of(roles));
        UserContext.set(user);
    }
}
