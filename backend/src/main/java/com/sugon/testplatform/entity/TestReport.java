package com.sugon.testplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("test_report")
public class TestReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private String fileName;
    private String fileKey;
    private Long fileSize;
    private Integer version;
    private Long uploadBy;
    private String uploadByName;
    private LocalDateTime createTime;
}
