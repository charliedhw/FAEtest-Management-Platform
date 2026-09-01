package com.sugon.testplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sugon.testplatform.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
