package com.sugon.testplatform.service.impl;

import com.sugon.testplatform.common.BizException;
import com.sugon.testplatform.dto.WordImportResult;
import com.sugon.testplatform.entity.SysUser;
import com.sugon.testplatform.entity.SysUserGroup;
import com.sugon.testplatform.entity.SysUserGroupRel;
import com.sugon.testplatform.mapper.SysUserGroupMapper;
import com.sugon.testplatform.mapper.SysUserGroupRelMapper;
import com.sugon.testplatform.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析Word测试申请表，映射到申请字段
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WordImportServiceImpl {
    private final SysUserMapper userMapper;
    private final SysUserGroupMapper userGroupMapper;
    private final SysUserGroupRelMapper userGroupRelMapper;

    // 日期解析模式
    private static final Pattern DATE_RANGE = Pattern.compile("(\\d{4})\\s*年\\s*(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*日\\s*(?:到|至|-)\\s*(\\d{4})\\s*年\\s*(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*日");
    // 匹配"姓名：张三  电话：13812345678"格式
    private static final Pattern CONTACT_PATTERN = Pattern.compile("姓名[:：]\\s*([\\u4e00-\\u9fa5]{2,4})");
    // 匹配"张三 13812345678"格式（姓名+手机号）
    private static final Pattern NAME_PHONE = Pattern.compile("([\\u4e00-\\u9fa5]{2,4})\\s+(1[3-9]\\d{9})");

    public WordImportResult parseWord(MultipartFile file) {
        WordImportResult result = new WordImportResult();
        Map<String, String> tableMap = new LinkedHashMap<>();

        try (InputStream is = file.getInputStream(); XWPFDocument doc = new XWPFDocument(is)) {
            // 解析所有表格
            String lastLabel = null; // 上一个有效标签（处理合并单元格）
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    List<XWPFTableCell> cells = row.getTableCells();
                    if (cells.size() < 2) continue;
                    String label = cleanText(cells.get(0).getText());
                    // 获取所有去重后的单元格文本
                    List<String> cellTexts = new ArrayList<>();
                    for (XWPFTableCell cell : cells) {
                        String t = cleanText(cell.getText());
                        if (StringUtils.hasText(t) && (cellTexts.isEmpty() || !t.equals(cellTexts.get(cellTexts.size() - 1)))) {
                            cellTexts.add(t);
                        }
                    }
                    if (cellTexts.isEmpty()) continue;

                    // 合并单元格：label为空时继承上一个label
                    if (!StringUtils.hasText(label) && lastLabel != null) {
                        label = lastLabel;
                    }
                    if (StringUtils.hasText(label)) {
                        lastLabel = label;
                    }

                    if (!StringUtils.hasText(label)) continue;
                    // 判断label是否来自cellTexts[0]（正常行）还是继承的（合并单元格）
                    boolean labelFromCell = StringUtils.hasText(cleanText(cells.get(0).getText()));

                    // 特殊处理：一行包含两组标签-值对（如：销售人员|值|方案售前|值）
                    if (labelFromCell && cellTexts.size() >= 4 && label.contains("销售") && findInList(cellTexts, "售前") >= 0) {
                        tableMap.put("销售人员", cellTexts.get(1));
                        int idx = findInList(cellTexts, "售前");
                        if (idx >= 0 && idx + 1 < cellTexts.size()) {
                            tableMap.put("方案售前", cellTexts.get(idx + 1));
                        }
                        continue;
                    }

                    // 特殊处理：测试资源子行
                    if (label.contains("测试资源")) {
                        // 获取子标签和值（跳过label本身如果在cellTexts[0]）
                        int startIdx = labelFromCell ? 1 : 0;
                        if (cellTexts.size() > startIdx + 1) {
                            // 有子标签+值
                            String subLabel = cellTexts.get(startIdx);
                            String subValue = cellTexts.get(cellTexts.size() - 1);
                            if (subLabel.contains("资源类型")) {
                                tableMap.put("资源类型", subValue);
                            } else if (subLabel.contains("配置") || subLabel.contains("测试机")) {
                                tableMap.put("测试机配置", subValue);
                            } else if (subLabel.contains("自测") || subLabel.contains("FAE")) {
                                tableMap.put("测试方式_raw", subValue);
                            } else {
                                // 子标签无法识别，用值内容推断
                                classifyTestResourceValue(subValue, tableMap);
                            }
                        } else if (cellTexts.size() > startIdx) {
                            // 只有一个值
                            String v = cellTexts.get(startIdx);
                            classifyTestResourceValue(v, tableMap);
                        }
                        continue;
                    }

                    if (cellTexts.size() < 2) continue;
                    String value = cellTexts.get(cellTexts.size() - 1);

                    // 普通行：标签不重复或值更长时更新
                    if (tableMap.containsKey(label)) {
                        String existing = tableMap.get(label);
                        if (value != null && value.length() > (existing != null ? existing.length() : 0)) {
                            tableMap.put(label, value);
                        }
                    } else {
                        tableMap.put(label, value);
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析Word文档失败", e);
            throw new BizException("解析Word文档失败：" + e.getMessage());
        }

        if (tableMap.isEmpty()) {
            throw new BizException("未从Word文档中解析到表格内容，请确认文档格式正确");
        }

        Map<String, Object> fields = result.getFields();
        List<String> warnings = result.getWarnings();

        // 用户名称 → customerName
        putField(fields, "customerName", tableMap.get("用户名称"));

        // 项目名称 → projectName
        putField(fields, "projectName", tableMap.get("项目名称"));

        // SPM编号 → spmNo
        putField(fields, "spmNo", tableMap.get("SPM编号"));

        // 项目情况 → requirement
        putField(fields, "requirement", tableMap.get("项目情况"));

        // 测试方案 → testPlan
        putField(fields, "testPlan", tableMap.get("测试方案"));

        // 测试机配置 → hardwareConfig
        String hwConfig = tableMap.get("测试机配置");
        if (hwConfig == null) hwConfig = findByPartialLabel(tableMap, "测试机配置");
        if (hwConfig == null) hwConfig = findByPartialLabel(tableMap, "配置");
        putField(fields, "hardwareConfig", hwConfig);

        // 测试时间 → testStartTime, testEndTime, applyDays
        String timeText = tableMap.get("测试时间");
        if (StringUtils.hasText(timeText)) {
            parseDateRange(timeText, fields, warnings);
        }

        // 资源类型 → expectResourceType
        String resourceType = tableMap.get("资源类型");
        if (StringUtils.hasText(resourceType)) {
            // 匹配字典中的资源类型
            String mapped = mapResourceType(resourceType);
            if (mapped != null) {
                fields.put("expectResourceType", mapped);
            } else {
                fields.put("expectResourceType", resourceType);
                warnings.add("资源类型「" + resourceType + "」未能精确匹配，请手动确认");
            }
        }

        // 测试方式（从测试资源子行解析）
        String testMethodText = tableMap.get("测试方式_raw");
        if (testMethodText == null) testMethodText = findByPartialLabel(tableMap, "自测");
        if (testMethodText == null) testMethodText = findByPartialLabel(tableMap, "FAE");
        if (StringUtils.hasText(testMethodText)) {
            String mapped = mapTestMethod(testMethodText, resourceType);
            if (mapped != null) {
                fields.put("testMethod", mapped);
            } else {
                warnings.add("测试方式「" + testMethodText + "」未能精确匹配，请手动确认");
            }
        }

        // 销售人员 → salesId, salesName
        String salesText = tableMap.get("销售人员");
        if (StringUtils.hasText(salesText)) {
            String salesName = extractName(salesText);
            if (StringUtils.hasText(salesName)) {
                fields.put("salesName", salesName);
                matchUser(salesName, "SALES_GROUP", fields, warnings, "salesId", "销售");
            }
        }

        // 方案售前 → presalesId, presalesName
        String presalesText = tableMap.get("方案售前");
        if (StringUtils.hasText(presalesText)) {
            String presalesName = extractName(presalesText);
            if (StringUtils.hasText(presalesName)) {
                fields.put("presalesName", presalesName);
                matchUser(presalesName, "PRESALES_GROUP", fields, warnings, "presalesId", "售前");
            }
        }

        // 用户联系人姓名（仅记录到warnings，供用户参考）
        String contact = tableMap.get("用户联系人");
        if (StringUtils.hasText(contact)) {
            Matcher m = CONTACT_PATTERN.matcher(contact);
            if (m.find()) {
                fields.put("contactPerson", m.group(1));
            }
        }

        // 设备类型：从硬件配置中推断
        if (hwConfig != null) {
            String deviceType = extractDeviceType(hwConfig);
            if (deviceType != null) {
                fields.put("deviceType", deviceType);
            }
        }

        // 测试类型：根据测试方案内容推断
        String testPlan = (String) fields.get("testPlan");
        if (StringUtils.hasText(testPlan)) {
            List<String> testTypes = inferTestTypes(testPlan);
            if (!testTypes.isEmpty()) {
                fields.put("testType", testTypes);
            }
        }

        // 软件及应用：从测试方案中提取
        if (StringUtils.hasText(testPlan)) {
            String softwareApp = extractSoftwareApp(testPlan);
            if (softwareApp != null) {
                fields.put("softwareApp", softwareApp);
            }
        }

        return result;
    }

    // ===== 辅助方法 =====

    private void putField(Map<String, Object> fields, String key, String value) {
        if (StringUtils.hasText(value)) {
            fields.put(key, value.trim());
        }
    }

    private String cleanText(String text) {
        if (text == null) return null;
        // 去除多余空白，保留换行
        return text.replaceAll("[ \\t]+", " ").replaceAll("\\n{3,}", "\n\n").trim();
    }

    /**
     * 根据值内容推断测试资源子字段类型
     */
    private void classifyTestResourceValue(String v, Map<String, String> tableMap) {
        if (v == null) return;
        if (v.contains("配置") || v.contains("ScaleX") || v.contains("BW") || v.contains("服务器") || v.contains("超节点")) {
            tableMap.put("测试机配置", v);
        } else if ((v.contains("线上") || v.contains("线下")) && v.contains("资源")) {
            tableMap.put("资源类型", v);
        } else if (v.contains("FAE") || v.contains("自测") || v.contains("支持")) {
            tableMap.put("测试方式_raw", v);
        } else if (v.contains("线上") || v.contains("线下")) {
            tableMap.put("资源类型", v);
        }
    }

    /**
     * 在列表中查找包含关键词的元素索引
     */
    private int findInList(List<String> list, String keyword) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).contains(keyword)) return i;
        }
        return -1;
    }

    /**
     * 部分标签匹配（处理合并单元格导致的标签不完整情况）
     */
    private String findByPartialLabel(Map<String, String> map, String partial) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (entry.getKey().contains(partial) && StringUtils.hasText(entry.getValue())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 解析测试时间范围 "2026 年 9 月 21 日 到 2026 年 10 月 20 日"
     */
    private void parseDateRange(String text, Map<String, Object> fields, List<String> warnings) {
        Matcher m = DATE_RANGE.matcher(text);
        if (m.find()) {
            try {
                LocalDate start = LocalDate.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
                LocalDate end = LocalDate.of(Integer.parseInt(m.group(4)), Integer.parseInt(m.group(5)), Integer.parseInt(m.group(6)));
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                fields.put("testStartTime", start.format(fmt));
                fields.put("testEndTime", end.format(fmt));
                long days = ChronoUnit.DAYS.between(start, end) + 1;
                fields.put("applyDays", (int) days);
            } catch (Exception e) {
                warnings.add("测试时间解析异常：" + text);
            }
        } else {
            warnings.add("未能从「测试时间」中解析出起止日期：" + text);
        }
    }

    /**
     * 从文本中提取姓名（去掉手机号等）
     */
    private String extractName(String text) {
        if (text == null) return null;
        // 先尝试匹配 "姓名：张三" 格式
        Matcher cm = CONTACT_PATTERN.matcher(text);
        if (cm.find()) return cm.group(1);
        // 匹配 "张三 13812345678" 格式
        Matcher pm = NAME_PHONE.matcher(text);
        if (pm.find()) return pm.group(1);
        // 直接取前2-4个中文字符
        Matcher nameM = Pattern.compile("^([\\u4e00-\\u9fa5]{2,4})").matcher(text.trim());
        if (nameM.find()) return nameM.group(1);
        return text.trim();
    }

    /**
     * 匹配用户（按姓名在用户组中查找）
     */
    private void matchUser(String name, String groupCode, Map<String, Object> fields, List<String> warnings, String idField, String roleLabel) {
        // 查找用户组
        SysUserGroup group = userGroupMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUserGroup>()
                        .eq(SysUserGroup::getGroupCode, groupCode));
        if (group == null) {
            warnings.add(roleLabel + "「" + name + "」未能匹配到系统用户（用户组不存在），请手动选择");
            return;
        }
        // 在组内查找用户
        List<SysUserGroupRel> rels = userGroupRelMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUserGroupRel>()
                        .eq(SysUserGroupRel::getGroupId, group.getId()));
        List<Long> userIds = rels.stream().map(SysUserGroupRel::getUserId).toList();
        if (userIds.isEmpty()) {
            warnings.add(roleLabel + "「" + name + "」未能匹配到系统用户（组内无用户），请手动选择");
            return;
        }
        List<SysUser> users = userMapper.selectBatchIds(userIds);
        // 精确匹配姓名
        SysUser matched = users.stream()
                .filter(u -> name.equals(u.getRealName()))
                .findFirst().orElse(null);
        // 模糊匹配
        if (matched == null) {
            matched = users.stream()
                    .filter(u -> u.getRealName() != null && (u.getRealName().contains(name) || name.contains(u.getRealName())))
                    .findFirst().orElse(null);
        }
        if (matched != null) {
            fields.put(idField, matched.getId());
            fields.put(idField.replace("Id", "Name"), matched.getRealName());
        } else {
            warnings.add(roleLabel + "「" + name + "」未能匹配到系统用户，请手动选择");
        }
    }

    /**
     * 映射资源类型到字典值
     */
    private String mapResourceType(String text) {
        if (text == null) return null;
        if (text.contains("线上")) return "线上资源";
        if (text.contains("线下")) return "线下资源";
        return null;
    }

    /**
     * 映射测试方式到字典值
     */
    private String mapTestMethod(String text, String resourceType) {
        if (text == null) return null;
        boolean isOnline = resourceType != null && resourceType.contains("线上");
        boolean isFae = text.contains("FAE");
        boolean isSelf = text.contains("自测");

        if (isFae && isOnline) return "线上FAE支持测试";
        if (isFae) return "线下FAE支持测试";
        if (isSelf && isOnline) return "线上测试用户自测";
        if (isSelf) return "线下用户自测";
        // 单独匹配
        if (text.contains("线上FAE")) return "线上FAE支持测试";
        if (text.contains("线上") && text.contains("自测")) return "线上测试用户自测";
        if (text.contains("线下FAE")) return "线下FAE支持测试";
        if (text.contains("线下") && text.contains("自测")) return "线下用户自测";
        return null;
    }

    /**
     * 从硬件配置中提取设备类型
     */
    private String extractDeviceType(String hwConfig) {
        if (hwConfig == null) return null;
        // 匹配常见设备型号
        Pattern[] patterns = {
                Pattern.compile("(ScaleX\\d+)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(BW\\d+)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(X86\\S*)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(GPU\\S*)", Pattern.CASE_INSENSITIVE)
        };
        for (Pattern p : patterns) {
            Matcher m = p.matcher(hwConfig);
            if (m.find()) return m.group(1);
        }
        return null;
    }

    /**
     * 从测试方案推断测试类型
     */
    private List<String> inferTestTypes(String testPlan) {
        List<String> types = new ArrayList<>();
        if (testPlan.contains("AI") || testPlan.contains("大模型") || testPlan.contains("算力") || testPlan.contains("推理") || testPlan.contains("GLM")) {
            types.add("AI");
        }
        if (testPlan.contains("性能") || testPlan.contains("压测") || testPlan.contains("高负载") || testPlan.contains("benchmark")) {
            types.add("性能");
        }
        if (testPlan.contains("稳定") || testPlan.contains("可靠性") || testPlan.contains("长稳")) {
            types.add("稳定性");
        }
        if (testPlan.contains("兼容") || testPlan.contains("适配")) {
            types.add("兼容性");
        }
        if (testPlan.contains("功能")) {
            types.add("功能");
        }
        if (types.isEmpty()) types.add("测试");
        return types;
    }

    /**
     * 从测试方案中提取软件及应用
     */
    private String extractSoftwareApp(String testPlan) {
        if (testPlan == null) return null;
        // 提取"测试目标"部分的内容
        int idx = testPlan.indexOf("测试目标");
        if (idx >= 0) {
            String after = testPlan.substring(idx + 4);
            // 去掉冒号
            after = after.replaceAll("^[:：]\\s*", "");
            // 取到下一个主要分段之前
            int endIdx = after.indexOf("测试计划");
            if (endIdx < 0) endIdx = after.indexOf("另附");
            if (endIdx > 0) {
                return after.substring(0, endIdx).trim();
            }
            return after.trim();
        }
        return null;
    }
}
