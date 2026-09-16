package com.sugon.testplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sugon.testplatform.entity.MailLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MailLogMapper extends BaseMapper<MailLog> {
}
