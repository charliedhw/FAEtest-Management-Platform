package com.sugon.testplatform.dto;

import lombok.Data;

@Data
public class ApplicationSubmitRequest {
    private Long id;
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
    private Long salesId;        // 关联销售
    private String salesName;
    private Long presalesId;     // 关联售前
    private String presalesName;
}
