package com.sugon.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sugon.testplatform.common.BizException;
import com.sugon.testplatform.common.PageResult;
import com.sugon.testplatform.entity.WeeklyReport;
import com.sugon.testplatform.mapper.WeeklyReportMapper;
import com.sugon.testplatform.security.UserContext;
import com.sugon.testplatform.service.WeeklyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class WeeklyReportServiceImpl implements WeeklyReportService {
    private final WeeklyReportMapper weeklyReportMapper;

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
}
