package com.sugon.testplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("test_application")
public class TestApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String appNo;
    private Long projectId;
    private Long applicantId;
    private String applicantName;
    private String customerName;
    private String projectName;
    private String region;
    private String spmNo;
    private String projectStage;
    private String bidStatus;
    private String requirement;
    private String testPlan;
    private String testType;
    private String deviceType;
    private String hardwareConfig;
    private String softwareApp;
    private String applyPeriod;
    private String expectResourceType;
    private String testMethod;
    private Integer applyDays;
    private Long salesId;
    private String salesName;
    private Long presalesId;
    private String presalesName;
    private String currentNode;
    private String status;
    private String rejectReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
