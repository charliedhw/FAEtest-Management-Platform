package com.sugon.testplatform.dto;

import lombok.Data;

@Data
public class ApprovalRequest {
    private Long appId;
    private String action;      // APPROVE / REJECT
    private String opinion;
    private String rejectReason;
}
