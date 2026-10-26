package com.sugon.testplatform.dto;

import lombok.Data;

import java.util.List;

@Data
public class PasswordResetRequest {
    private List<Long> userIds;
    private String newPassword;
}
