package com.sugon.testplatform.service;

import com.sugon.testplatform.common.PageResult;
import com.sugon.testplatform.entity.WeeklyReport;

import java.util.List;

public interface WeeklyReportService {
    void save(WeeklyReport report);
    void delete(Long id);
    WeeklyReport detail(Long id);
    PageResult<WeeklyReport> page(int pageNum, int pageSize, Integer weekNum, Integer year, String authorName);
    // 查询当前用户最近的周报（判断本周是否已提交）
    WeeklyReport getMyLatest();
    // 获取当前周数
    int currentWeekNum();
    int currentYear();
}
