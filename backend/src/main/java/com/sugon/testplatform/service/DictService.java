package com.sugon.testplatform.service;

import com.sugon.testplatform.entity.SysDict;
import java.util.List;
import java.util.Map;

public interface DictService {
    List<SysDict> listByType(String dictType);
    Map<String, List<SysDict>> listAll();
    void save(SysDict dict);
    void delete(Long id);
    String getConfig(String key, String defaultValue);
}
