package com.sugon.testplatform.controller;

import com.sugon.testplatform.common.PageResult;
import com.sugon.testplatform.common.Result;
import com.sugon.testplatform.entity.TestProject;
import com.sugon.testplatform.service.ExportService;
import com.sugon.testplatform.service.ProjectService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/project")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;
    private final ExportService exportService;

    @GetMapping("/page")
    public Result<PageResult<TestProject>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                @RequestParam(defaultValue = "10") int pageSize,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) String region,
                                                @RequestParam(required = false) String testType,
                                                @RequestParam(required = false) String tester,
                                                @RequestParam(required = false) String deviceType,
                                                @RequestParam(required = false) String period,
                                                @RequestParam(required = false) String testStartFrom,
                                                @RequestParam(required = false) String testStartTo,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Long salesId,
                                                @RequestParam(required = false) Long presalesId,
                                                @RequestParam(required = false) Integer isKeyProject,
                                                @RequestParam(required = false) String bizType) {
        Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        params.put("region", region);
        params.put("testType", testType);
        params.put("tester", tester);
        params.put("deviceType", deviceType);
        params.put("period", period);
        params.put("testStartFrom", testStartFrom);
        params.put("testStartTo", testStartTo);
        params.put("keyword", keyword);
        params.put("salesId", salesId);
        params.put("presalesId", presalesId);
        params.put("isKeyProject", isKeyProject);
        params.put("bizType", bizType);
        return Result.ok(projectService.page(pageNum, pageSize, params));
    }

    @GetMapping("/{id}")
    public Result<TestProject> detail(@PathVariable Long id) {
        return Result.ok(projectService.detail(id));
    }

    @PostMapping("/update")
    public Result<Void> update(@RequestBody TestProject project) {
        projectService.update(project);
        return Result.ok();
    }

    @PostMapping("/updateStatus")
    public Result<Void> updateStatus(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(body.get("id").toString());
        projectService.updateStatus(id, body.get("status").toString());
        return Result.ok();
    }

    @PostMapping("/updateBid")
    public Result<Void> updateBid(@RequestBody Map<String, Object> body) {
        Long id = Long.valueOf(body.get("id").toString());
        String bidStatus = body.get("bidStatus") == null ? null : body.get("bidStatus").toString();
        BigDecimal bidAmount = body.get("bidAmount") == null || body.get("bidAmount").toString().isEmpty()
                ? null : new BigDecimal(body.get("bidAmount").toString());
        projectService.updateBid(id, bidStatus, bidAmount);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return Result.ok();
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String region) {
        Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        params.put("region", region);
        exportService.exportProjects(response, params);
    }
}
