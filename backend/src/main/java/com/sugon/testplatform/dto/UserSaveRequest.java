package com.sugon.testplatform.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserSaveRequest {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String email;
    private String phone;
    private Long deptId;
    private List<Long> roleIds;
    private Integer status;
}
