package com.sugon.testplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("resource_loan")
public class ResourceLoan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long resourceId;
    private Long projectId;
    private Long appId;
    private Long borrowerId;
    private String borrowerName;
    private Long deptId;
    private String deptName;
    private LocalDateTime loanTime;
    private LocalDateTime expectReturnTime;
    private LocalDateTime actualReturnTime;
    private Integer loanDays;
    private String status;
    private BigDecimal costAmount;
    private String remark;
    private LocalDateTime createTime;
}
