package com.sugon.testplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("test_project")
public class TestProject {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String projectNo;
    private String customerName;
    private String projectName;
    private String region;
    private String spmNo;
    private String projectStage;
    private Long salesId;
    private String salesName;
    private Long presalesId;
    private String presalesName;
    private String testerIds;
    private String testerNames;
    private String testPlan;
    private String testType;
    private String deviceType;
    private String hardwareConfig;
    private String softwareApp;
    private String isInternalResource;
    private String testMethod;
    private LocalDateTime applyTime;
    private String applyPeriod;
    private LocalDate testStartTime;
    private LocalDate testEndTime;
    private String testConclusion;
    private String status;
    private LocalDateTime updateTime;
    private String reportLink;
    private String bidStatus;
    private BigDecimal bidAmount;
    private Integer isKeyProject;
    private String bizType;
    private Long createBy;
    private LocalDateTime createTime;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private java.util.Map<String, Boolean> permissions;
}
