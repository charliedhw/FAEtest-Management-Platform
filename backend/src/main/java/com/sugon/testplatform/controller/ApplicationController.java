package com.sugon.testplatform.controller;

import com.sugon.testplatform.common.PageResult;
import com.sugon.testplatform.common.Result;
import com.sugon.testplatform.dto.ApplicationSubmitRequest;
import com.sugon.testplatform.dto.ApprovalRequest;
import com.sugon.testplatform.dto.AssignRequest;
import com.sugon.testplatform.dto.WordImportResult;
import com.sugon.testplatform.entity.TestApplication;
import com.sugon.testplatform.security.UserContext;
import com.sugon.testplatform.service.ApplicationService;
import com.sugon.testplatform.service.impl.WordImportServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/application")
@RequiredArgsConstructor
public class ApplicationController {
    private final ApplicationService applicationService;
    private final WordImportServiceImpl wordImportService;

    @PostMapping("/importWord")
    public Result<WordImportResult> importWord(@RequestParam("file") MultipartFile file) {
        return Result.ok(wordImportService.parseWord(file));
    }

    @PostMapping("/submit")
    public Result<Long> submit(@RequestBody ApplicationSubmitRequest req) {
        return Result.ok(applicationService.submit(req));
    }

    @PostMapping("/draft")
    public Result<Void> draft(@RequestBody ApplicationSubmitRequest req) {
        applicationService.saveDraft(req);
        return Result.ok();
    }

    @PostMapping("/approve")
    public Result<Void> approve(@RequestBody ApprovalRequest req) {
        applicationService.approve(req);
        return Result.ok();
    }

    @PostMapping("/assign")
    public Result<Void> assign(@RequestBody AssignRequest req) {
        applicationService.assign(req);
        return Result.ok();
    }

    @PostMapping("/withdraw/{id}")
    public Result<Void> withdraw(@PathVariable Long id) {
        applicationService.withdraw(id);
        return Result.ok();
    }

    @GetMapping("/page")
    public Result<PageResult<TestApplication>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                    @RequestParam(defaultValue = "10") int pageSize,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) Long applicantId) {
        return Result.ok(applicationService.page(pageNum, pageSize, status, keyword, applicantId, false));
    }

    @GetMapping("/todo")
    public Result<PageResult<TestApplication>> todo(@RequestParam(defaultValue = "1") int pageNum,
                                                    @RequestParam(defaultValue = "10") int pageSize) {
        return Result.ok(applicationService.todoList(UserContext.getUserId(), pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public Result<TestApplication> detail(@PathVariable Long id) {
        return Result.ok(applicationService.detail(id));
    }
}
