package com.sugon.testplatform.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TestReportView {
    private Long id;
    private Long projectId;
    private String fileName;
    private Long fileSize;
    private Integer version;
    private Long uploadBy;
    private String uploadByName;
    private LocalDateTime createTime;
    private String projectNo;
    private String projectName;
    private String customerName;
    private String testerNames;
    private String testType;
    private String deviceType;
    private String projectStatus;
}
