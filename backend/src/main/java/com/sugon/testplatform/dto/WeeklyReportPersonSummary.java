package com.sugon.testplatform.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 周报人员维度汇总（供审批组按人查看每位FAE工程师的周报情况）
 */
@Data
public class WeeklyReportPersonSummary {
    private Long userId;
    private String realName;
    // 是否已提交指定周的周报
    private Boolean submitted;
    private Long reportId;
    private String title;
    // 本周进展摘要（截取前200字）
    private String progressSummary;
    // 存在问题是否非空
    private Boolean hasProblems;
    private String problemSummary;
    private String nextPlanSummary;
    // 该工程师累计提交周报份数
    private Long reportCount;
    private LocalDateTime submitTime;
}
