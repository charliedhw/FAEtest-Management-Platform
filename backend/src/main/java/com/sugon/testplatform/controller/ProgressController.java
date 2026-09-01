package com.sugon.testplatform.controller;

import com.sugon.testplatform.common.PageResult;
import com.sugon.testplatform.common.Result;
import com.sugon.testplatform.dto.ProgressRequest;
import com.sugon.testplatform.entity.TestProgress;
import com.sugon.testplatform.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/progress")
@RequiredArgsConstructor
public class ProgressController {
    private final ProgressService progressService;

    @GetMapping("/summary")
    public Result<PageResult<Map<String, Object>>> summary(@RequestParam(defaultValue = "1") int pageNum,
                                                            @RequestParam(defaultValue = "20") int pageSize,
                                                            @RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) String date) {
        return Result.ok(progressService.projectSummary(pageNum, pageSize, keyword, date));
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestBody ProgressRequest req) {
        progressService.add(req);
        return Result.ok();
    }

    @PostMapping("/update")
    public Result<Void> update(@RequestBody TestProgress progress) {
        progressService.update(progress);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        progressService.delete(id);
        return Result.ok();
    }

    @GetMapping("/list/{projectId}")
    public Result<List<TestProgress>> list(@PathVariable Long projectId) {
        return Result.ok(progressService.listByProject(projectId));
    }
}
