package com.sugon.testplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("test_stage")
public class TestStage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String stageName;
    private String testDesc;
    private LocalDate planStart;
    private LocalDate planEnd;
    private String status;
    private Integer sort;
    private String remark;
    private Long createBy;
    private String createByName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Integer progressCount;
}
