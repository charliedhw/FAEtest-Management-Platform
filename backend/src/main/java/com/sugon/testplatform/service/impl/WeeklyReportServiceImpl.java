package com.sugon.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sugon.testplatform.common.BizException;
import com.sugon.testplatform.common.PageResult;
import com.sugon.testplatform.dto.WeeklyReportPersonSummary;
import com.sugon.testplatform.entity.SysUser;
import com.sugon.testplatform.entity.SysUserGroup;
import com.sugon.testplatform.entity.SysUserGroupRel;
import com.sugon.testplatform.entity.WeeklyReport;
import com.sugon.testplatform.mapper.SysUserGroupMapper;
import com.sugon.testplatform.mapper.SysUserGroupRelMapper;
import com.sugon.testplatform.mapper.SysUserMapper;
import com.sugon.testplatform.mapper.WeeklyReportMapper;
import com.sugon.testplatform.security.UserContext;
import com.sugon.testplatform.service.WeeklyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WeeklyReportServiceImpl implements WeeklyReportService {
    private static final String FAE_GROUP_CODE = "FAE_GROUP";
    private static final int SUMMARY_LENGTH = 200;

    private final WeeklyReportMapper weeklyReportMapper;
    private final SysUserGroupMapper userGroupMapper;
    private final SysUserGroupRelMapper userGroupRelMapper;
    private final SysUserMapper userMapper;

    @Override
    public void save(WeeklyReport report) {
        // 校验标题格式
        if (!StringUtils.hasText(report.getTitle())) {
            throw new BizException("周报名称不能为空");
        }
        if (report.getWeekNum() == null || report.getYear() == null) {
            throw new BizException("周数和年份不能为空");
        }
        if (!StringUtils.hasText(report.getThisWeekProgress())) {
            throw new BizException("请填写本周测试进展");
        }
        if (!StringUtils.hasText(report.getNextWeekPlan())) {
            throw new BizException("请填写下周工作计划");
        }

        Long userId = UserContext.getUserId();
        String realName = UserContext.getRealName();

        if (report.getId() != null) {
            // 更新：只能修改自己的周报
            WeeklyReport old = weeklyReportMapper.selectById(report.getId());
            if (old == null) throw new BizException("周报不存在");
            if (!old.getAuthorId().equals(userId)) {
                throw new BizException("只能修改自己的周报");
            }
            report.setAuthorId(old.getAuthorId());
            report.setAuthorName(old.getAuthorName());
            report.setUpdateTime(LocalDateTime.now());
            weeklyReportMapper.updateById(report);
        } else {
            // 新增：检查同周是否已提交
            WeeklyReport existing = weeklyReportMapper.selectOne(
                    new LambdaQueryWrapper<WeeklyReport>()
                            .eq(WeeklyReport::getAuthorId, userId)
                            .eq(WeeklyReport::getYear, report.getYear())
                            .eq(WeeklyReport::getWeekNum, report.getWeekNum()));
            if (existing != null) {
                throw new BizException("本周已提交过周报，可直接编辑修改");
            }
            report.setAuthorId(userId);
            report.setAuthorName(realName);
            report.setDeptName("应用测试部");
            report.setCreateTime(LocalDateTime.now());
            report.setUpdateTime(LocalDateTime.now());
            weeklyReportMapper.insert(report);
        }
    }

    @Override
    public void delete(Long id) {
        WeeklyReport old = weeklyReportMapper.selectById(id);
        if (old == null) throw new BizException("周报不存在");
        if (!old.getAuthorId().equals(UserContext.getUserId())) {
            throw new BizException("只能删除自己的周报");
        }
        weeklyReportMapper.deleteById(id);
    }

    @Override
    public WeeklyReport detail(Long id) {
        return weeklyReportMapper.selectById(id);
    }

    @Override
    public PageResult<WeeklyReport> page(int pageNum, int pageSize, Integer weekNum, Integer year, String authorName) {
        LambdaQueryWrapper<WeeklyReport> wrapper = new LambdaQueryWrapper<>();
        if (weekNum != null) wrapper.eq(WeeklyReport::getWeekNum, weekNum);
        if (year != null) wrapper.eq(WeeklyReport::getYear, year);
        if (StringUtils.hasText(authorName)) wrapper.like(WeeklyReport::getAuthorName, authorName);
        wrapper.orderByDesc(WeeklyReport::getYear).orderByDesc(WeeklyReport::getWeekNum).orderByDesc(WeeklyReport::getCreateTime);

        Page<WeeklyReport> page = weeklyReportMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    @Override
    public WeeklyReport getMyLatest() {
        return weeklyReportMapper.selectOne(
                new LambdaQueryWrapper<WeeklyReport>()
                        .eq(WeeklyReport::getAuthorId, UserContext.getUserId())
                        .orderByDesc(WeeklyReport::getYear)
                        .orderByDesc(WeeklyReport::getWeekNum)
                        .last("LIMIT 1"));
    }

    @Override
    public int currentWeekNum() {
        return java.time.LocalDate.now().get(WeekFields.of(Locale.CHINA).weekOfWeekBasedYear());
    }

    @Override
    public int currentYear() {
        return java.time.LocalDate.now().getYear();
    }

    @Override
    public List<WeeklyReportPersonSummary> personSummary(Integer year, Integer weekNum) {
        if (year == null) year = currentYear();
        if (weekNum == null) weekNum = currentWeekNum();

        // 1. 查询FAE测试组全部成员
        SysUserGroup group = userGroupMapper.selectOne(
                new LambdaQueryWrapper<SysUserGroup>().eq(SysUserGroup::getGroupCode, FAE_GROUP_CODE));
        if (group == null) {
            return Collections.emptyList();
        }
        List<SysUserGroupRel> rels = userGroupRelMapper.selectList(
                new LambdaQueryWrapper<SysUserGroupRel>().eq(SysUserGroupRel::getGroupId, group.getId()));
        if (rels.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> userIds = rels.stream().map(SysUserGroupRel::getUserId).collect(Collectors.toList());
        List<SysUser> members = userMapper.selectBatchIds(userIds).stream()
                .filter(u -> u.getStatus() == null || u.getStatus() == 1)
                .collect(Collectors.toList());
        if (members.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 查询指定周的周报，按作者分组
        List<WeeklyReport> weekReports = weeklyReportMapper.selectList(
                new LambdaQueryWrapper<WeeklyReport>()
                        .eq(WeeklyReport::getYear, year)
                        .eq(WeeklyReport::getWeekNum, weekNum));
        Map<Long, WeeklyReport> reportMap = weekReports.stream()
                .collect(Collectors.toMap(WeeklyReport::getAuthorId, r -> r, (a, b) -> a));

        // 3. 查询每位成员累计周报数
        List<Map<String, Object>> counts = weeklyReportMapper.selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<WeeklyReport>()
                        .select("author_id AS authorId", "COUNT(*) AS cnt")
                        .in("author_id", members.stream().map(SysUser::getId).collect(Collectors.toList()))
                        .groupBy("author_id"));
        Map<Long, Long> countMap = counts.stream().collect(Collectors.toMap(
                m -> ((Number) m.get("authorId")).longValue(),
                m -> ((Number) m.get("cnt")).longValue()));

        // 4. 组装结果：未提交/有问题的排前面
        List<WeeklyReportPersonSummary> result = new ArrayList<>();
        for (SysUser member : members) {
            WeeklyReportPersonSummary s = new WeeklyReportPersonSummary();
            s.setUserId(member.getId());
            s.setRealName(member.getRealName());
            s.setReportCount(countMap.getOrDefault(member.getId(), 0L));
            WeeklyReport report = reportMap.get(member.getId());
            if (report != null) {
                s.setSubmitted(true);
                s.setReportId(report.getId());
                s.setTitle(report.getTitle());
                s.setProgressSummary(truncate(report.getThisWeekProgress()));
                s.setHasProblems(StringUtils.hasText(report.getProblems()));
                s.setProblemSummary(truncate(report.getProblems()));
                s.setNextPlanSummary(truncate(report.getNextWeekPlan()));
                s.setSubmitTime(report.getCreateTime());
            } else {
                s.setSubmitted(false);
                s.setHasProblems(false);
            }
            result.add(s);
        }
        result.sort((a, b) -> {
            // 未提交优先，其次有问题的优先，再按姓名排序
            int c = Boolean.compare(a.getSubmitted(), b.getSubmitted());
            if (c != 0) return c;
            c = Boolean.compare(Boolean.TRUE.equals(b.getHasProblems()), Boolean.TRUE.equals(a.getHasProblems()));
            if (c != 0) return c;
            String na = a.getRealName() == null ? "" : a.getRealName();
            String nb = b.getRealName() == null ? "" : b.getRealName();
            return na.compareTo(nb);
        });
        return result;
    }

    private String truncate(String text) {
        if (!StringUtils.hasText(text)) return null;
        return text.length() <= SUMMARY_LENGTH ? text : text.substring(0, SUMMARY_LENGTH) + "...";
    }
}
