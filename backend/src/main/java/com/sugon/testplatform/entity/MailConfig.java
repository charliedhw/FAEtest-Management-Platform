package com.sugon.testplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("mail_config")
public class MailConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String fromAddr;
    private String fromName;
    private Integer useSsl;
    private Integer enabled;
    private LocalDateTime updateTime;
}
