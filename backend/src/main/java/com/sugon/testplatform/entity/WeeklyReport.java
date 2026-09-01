package com.sugon.testplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("weekly_report")
public class WeeklyReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private Integer weekNum;
    private Integer year;
    private String thisWeekProgress;
    private String problems;
    private String nextWeekPlan;
    private Long authorId;
    private String authorName;
    private String deptName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
