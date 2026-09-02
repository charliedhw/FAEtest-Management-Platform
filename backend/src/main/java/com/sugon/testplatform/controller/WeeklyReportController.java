package com.sugon.testplatform.controller;

import com.sugon.testplatform.common.PageResult;
import com.sugon.testplatform.common.Result;
import com.sugon.testplatform.dto.WeeklyReportPersonSummary;
import com.sugon.testplatform.entity.WeeklyReport;
import com.sugon.testplatform.service.WeeklyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/weeklyReport")
@RequiredArgsConstructor
public class WeeklyReportController {
    private final WeeklyReportService weeklyReportService;

    @PostMapping("/save")
    public Result<Void> save(@RequestBody WeeklyReport report) {
        weeklyReportService.save(report);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        weeklyReportService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}")
    public Result<WeeklyReport> detail(@PathVariable Long id) {
        return Result.ok(weeklyReportService.detail(id));
    }

    @GetMapping("/page")
    public Result<PageResult<WeeklyReport>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                  @RequestParam(defaultValue = "20") int pageSize,
                                                  @RequestParam(required = false) Integer weekNum,
                                                  @RequestParam(required = false) Integer year,
                                                  @RequestParam(required = false) String authorName) {
        return Result.ok(weeklyReportService.page(pageNum, pageSize, weekNum, year, authorName));
    }

    @GetMapping("/myLatest")
    public Result<WeeklyReport> myLatest() {
        return Result.ok(weeklyReportService.getMyLatest());
    }

    @GetMapping("/currentWeek")
    public Result<Map<String, Integer>> currentWeek() {
        Map<String, Integer> map = new HashMap<>();
        map.put("weekNum", weeklyReportService.currentWeekNum());
        map.put("year", weeklyReportService.currentYear());
        return Result.ok(map);
    }

    @GetMapping("/personSummary")
    public Result<List<WeeklyReportPersonSummary>> personSummary(@RequestParam(required = false) Integer year,
                                                                  @RequestParam(required = false) Integer weekNum) {
        return Result.ok(weeklyReportService.personSummary(year, weekNum));
    }
}
