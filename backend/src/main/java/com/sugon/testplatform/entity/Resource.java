package com.sugon.testplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("resource")
public class Resource {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String resourceCode;
    private String serialNo;
    private String resourceName;
    private String resourceType;
    private String hardwareConfig;
    private BigDecimal factoryPrice;
    private Long deptId;
    private String deptName;
    private String status;
    private String onlineStatus;
    private String location;
    private String remark;
    private LocalDateTime createTime;
}
