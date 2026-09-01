package com.sugon.testplatform.security;

import lombok.Data;
import java.util.List;

@Data
public class LoginUser {
    private Long userId;
    private String username;
    private String realName;
    private List<String> roles;

    public boolean hasRole(String code) {
        return roles != null && roles.contains(code);
    }
}
