package com.sugon.testplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("test_progress")
public class TestProgress {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long stageId;
    private LocalDate progressDate;
    private String content;
    private Long createBy;
    private String createByName;
    private LocalDateTime createTime;
}
