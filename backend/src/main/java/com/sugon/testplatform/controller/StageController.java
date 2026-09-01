package com.sugon.testplatform.controller;

import com.sugon.testplatform.common.Result;
import com.sugon.testplatform.entity.TestStage;
import com.sugon.testplatform.service.StageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/stage")
@RequiredArgsConstructor
public class StageController {
    private final StageService stageService;

    @PostMapping("/add")
    public Result<TestStage> add(@RequestBody TestStage stage) {
        return Result.ok(stageService.add(stage));
    }

    @PostMapping("/update")
    public Result<Void> update(@RequestBody TestStage stage) {
        stageService.update(stage);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        stageService.delete(id);
        return Result.ok();
    }

    @GetMapping("/list/{projectId}")
    public Result<List<TestStage>> list(@PathVariable Long projectId) {
        return Result.ok(stageService.listByProject(projectId));
    }

    @GetMapping("/progress/{projectId}")
    public Result<Map<String, Object>> progress(@PathVariable Long projectId) {
        return Result.ok(stageService.projectProgress(projectId));
    }
}
