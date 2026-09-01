package com.sugon.testplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sugon.testplatform.entity.Resource;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResourceMapper extends BaseMapper<Resource> {
}
