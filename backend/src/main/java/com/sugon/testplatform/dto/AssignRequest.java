package com.sugon.testplatform.dto;

import lombok.Data;

@Data
public class AssignRequest {
    private Long appId;
    private String testerIds;      // 逗号分隔
    private String resourceType;   // 循环机/线上资源/其他资源/SPM
    private Long resourceId;
}
