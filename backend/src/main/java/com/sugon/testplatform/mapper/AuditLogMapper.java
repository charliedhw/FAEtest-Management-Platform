package com.sugon.testplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sugon.testplatform.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
