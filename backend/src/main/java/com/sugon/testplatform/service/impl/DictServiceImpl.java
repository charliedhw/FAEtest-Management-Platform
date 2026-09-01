package com.sugon.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sugon.testplatform.entity.SysConfig;
import com.sugon.testplatform.entity.SysDict;
import com.sugon.testplatform.mapper.SysConfigMapper;
import com.sugon.testplatform.mapper.SysDictMapper;
import com.sugon.testplatform.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DictServiceImpl implements DictService {
    private final SysDictMapper dictMapper;
    private final SysConfigMapper configMapper;

    @Override
    public List<SysDict> listByType(String dictType) {
        return dictMapper.selectList(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getDictType, dictType)
                .eq(SysDict::getStatus, 1)
                .orderByAsc(SysDict::getSort));
    }

    @Override
    public Map<String, List<SysDict>> listAll() {
        List<SysDict> all = dictMapper.selectList(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getStatus, 1).orderByAsc(SysDict::getSort));
        return all.stream().collect(Collectors.groupingBy(SysDict::getDictType));
    }

    @Override
    public void save(SysDict dict) {
        if (dict.getId() == null) {
            dictMapper.insert(dict);
        } else {
            dictMapper.updateById(dict);
        }
    }

    @Override
    public void delete(Long id) {
        dictMapper.deleteById(id);
    }

    @Override
    public String getConfig(String key, String defaultValue) {
        SysConfig cfg = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key));
        return cfg == null ? defaultValue : cfg.getConfigValue();
    }
}
