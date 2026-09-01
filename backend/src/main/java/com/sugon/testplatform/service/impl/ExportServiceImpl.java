package com.sugon.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sugon.testplatform.entity.TestProject;
import com.sugon.testplatform.mapper.TestProjectMapper;
import com.sugon.testplatform.service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {
    private final TestProjectMapper projectMapper;

    private static final String[] HEADERS = {
            "客户名称", "项目名称", "所属区域", "项目SPM号", "销售", "方案售前", "测试人员",
            "测试计划及内容", "测试类型", "设备类型", "硬件配置", "软件及应用", "是否内部测试资源",
            "申请时间", "申请测试周期（天）", "测试开始时间", "测试结束时间", "测试结论",
            "测试状态", "更新时间", "最新进展", "上传测试报告链接", "项目中标", "中标金额（万元）"
    };

    @Override
    public void exportProjects(HttpServletResponse response, Map<String, Object> params) {
        LambdaQueryWrapper<TestProject> qw = new LambdaQueryWrapper<>();
        if (params != null) {
            Object status = params.get("status");
            if (status != null && StringUtils.hasText(status.toString())) {
                qw.eq(TestProject::getStatus, status);
            }
            Object region = params.get("region");
            if (region != null && StringUtils.hasText(region.toString())) {
                qw.eq(TestProject::getRegion, region);
            }
        }
        qw.orderByDesc(TestProject::getUpdateTime);
        List<TestProject> list = projectMapper.selectList(qw);

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("测试项目汇总");
            // header
            Row header = sheet.createRow(0);
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 15 * 256);
            }
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            int rowNum = 1;
            for (TestProject p : list) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                row.createCell(c++).setCellValue(nvl(p.getCustomerName()));
                row.createCell(c++).setCellValue(nvl(p.getProjectName()));
                row.createCell(c++).setCellValue(nvl(p.getRegion()));
                row.createCell(c++).setCellValue(nvl(p.getSpmNo()));
                row.createCell(c++).setCellValue(nvl(p.getSalesName()));
                row.createCell(c++).setCellValue(nvl(p.getPresalesName()));
                row.createCell(c++).setCellValue(nvl(p.getTesterNames()));
                row.createCell(c++).setCellValue(nvl(p.getTestPlan()));
                row.createCell(c++).setCellValue(nvl(p.getTestType()));
                row.createCell(c++).setCellValue(nvl(p.getDeviceType()));
                row.createCell(c++).setCellValue(nvl(p.getHardwareConfig()));
                row.createCell(c++).setCellValue(nvl(p.getSoftwareApp()));
                row.createCell(c++).setCellValue(nvl(p.getIsInternalResource()));
                row.createCell(c++).setCellValue(p.getApplyTime() == null ? "" : p.getApplyTime().format(dtf));
                row.createCell(c++).setCellValue(nvl(p.getApplyPeriod()));
                row.createCell(c++).setCellValue(p.getTestStartTime() == null ? "" : p.getTestStartTime().format(df));
                row.createCell(c++).setCellValue(p.getTestEndTime() == null ? "" : p.getTestEndTime().format(df));
                row.createCell(c++).setCellValue(nvl(p.getTestConclusion()));
                row.createCell(c++).setCellValue(statusText(p.getStatus()));
                row.createCell(c++).setCellValue(p.getUpdateTime() == null ? "" : p.getUpdateTime().format(dtf));
                row.createCell(c++).setCellValue("");  // 最新进展
                row.createCell(c++).setCellValue(nvl(p.getReportLink()));
                row.createCell(c++).setCellValue(nvl(p.getBidStatus()));
                row.createCell(c++).setCellValue(p.getBidAmount() == null ? "" : p.getBidAmount().toString());
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String fileName = URLEncoder.encode("测试项目汇总.xlsx", StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            wb.write(response.getOutputStream());
        } catch (Exception e) {
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }

    private String nvl(String s) { return s == null ? "" : s; }

    private String statusText(String status) {
        if (status == null) return "";
        return switch (status) {
            case "NOT_START" -> "未开始";
            case "IN_PROGRESS" -> "进行中";
            case "PAUSED" -> "暂停";
            case "COMPLETED" -> "已完成";
            case "CLOSED" -> "关闭";
            case "REJECTED" -> "已驳回";
            default -> status;
        };
    }
}
