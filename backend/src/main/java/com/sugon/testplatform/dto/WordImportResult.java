package com.sugon.testplatform.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Word申请表导入解析结果
 */
@Data
public class WordImportResult {
    // 解析出的字段（key=字段名, value=解析值），直接对应 ApplicationSubmitRequest 字段
    private Map<String, Object> fields = new LinkedHashMap<>();
    // 需要人工确认的提示信息（如：人员未匹配、日期解析失败等）
    private java.util.List<String> warnings = new java.util.ArrayList<>();
}
