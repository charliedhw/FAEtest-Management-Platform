package com.sugon.testplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("mail_log")
public class MailLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String toAddr;
    private String subject;
    private String content;
    private String bizType;
    private Long bizId;
    private Integer success;
    private String error;
    private LocalDateTime createTime;
}
