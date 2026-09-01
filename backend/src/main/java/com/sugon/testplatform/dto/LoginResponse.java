package com.sugon.testplatform.dto;

import lombok.Data;
import java.util.List;

@Data
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String realName;
    private List<String> roles;
    private Integer isFirstLogin;
}
