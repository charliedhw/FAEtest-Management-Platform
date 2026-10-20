package com.sugon.testplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user_group")
public class SysUserGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String groupCode;
    private String groupName;
    private String groupType;   // GENERAL普通组 / PRESALES_ROOT售前大组 / PRESALES_DEPT售前业务部 / PRESALES_REGION售前置小组
    private Long parentId;      // 父组ID（售前大组->业务部->子组）
    private Long leaderId;
    private String leaderName;
    private String remark;
    private LocalDateTime createTime;
}
