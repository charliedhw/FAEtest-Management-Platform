package com.sugon.testplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("test_attachment")
public class TestAttachment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizType;      // APPLICATION
    private Long bizId;          // 申请单id
    private String fileType;     // METRIC测试指标要求 / PLAN测试方案
    private String fileName;
    private String fileKey;
    private Long fileSize;
    private Long uploadBy;
    private String uploadByName;
    private LocalDateTime createTime;
}
