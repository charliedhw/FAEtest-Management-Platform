package com.sugon.testplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_user_group_rel")
public class SysUserGroupRel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long groupId;
}
