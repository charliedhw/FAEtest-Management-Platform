package com.sugon.testplatform.controller;

import com.sugon.testplatform.common.Result;
import com.sugon.testplatform.entity.SysDict;
import com.sugon.testplatform.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dict")
@RequiredArgsConstructor
public class DictController {
    private final DictService dictService;

    @GetMapping("/type/{type}")
    public Result<List<SysDict>> listByType(@PathVariable String type) {
        return Result.ok(dictService.listByType(type));
    }

    @GetMapping("/all")
    public Result<Map<String, List<SysDict>>> all() {
        return Result.ok(dictService.listAll());
    }

    @PostMapping("/save")
    public Result<Void> save(@RequestBody SysDict dict) {
        dictService.save(dict);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dictService.delete(id);
        return Result.ok();
    }
}
