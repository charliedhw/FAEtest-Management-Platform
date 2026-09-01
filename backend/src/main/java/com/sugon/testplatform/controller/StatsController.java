package com.sugon.testplatform.controller;

import com.sugon.testplatform.common.Result;
import com.sugon.testplatform.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {
    private final StatsService statsService;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.ok(statsService.dashboard());
    }

    @GetMapping("/dimension")
    public Result<Map<String, Object>> dimension(@RequestParam String dimension) {
        return Result.ok(statsService.dimensionStats(dimension));
    }

    @GetMapping("/timeDimension")
    public Result<Map<String, Object>> timeDimension(@RequestParam(defaultValue = "month") String timeUnit) {
        return Result.ok(statsService.timeDimensionStats(timeUnit));
    }
}
