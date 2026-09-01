package com.sugon.testplatform.controller;

import com.sugon.testplatform.common.Result;
import com.sugon.testplatform.entity.TestReport;
import com.sugon.testplatform.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @PostMapping("/upload")
    public Result<TestReport> upload(@RequestParam Long projectId, @RequestParam MultipartFile file) {
        return Result.ok(reportService.upload(projectId, file));
    }

    @GetMapping("/list/{projectId}")
    public Result<List<TestReport>> list(@PathVariable Long projectId) {
        return Result.ok(reportService.listByProject(projectId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        reportService.delete(id);
        return Result.ok();
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        TestReport report = reportService.getById(id);
        byte[] data = reportService.download(id);
        String fileName = URLEncoder.encode(report.getFileName(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}
