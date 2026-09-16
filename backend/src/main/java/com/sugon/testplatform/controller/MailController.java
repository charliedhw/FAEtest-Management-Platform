package com.sugon.testplatform.controller;

import com.sugon.testplatform.common.BizException;
import com.sugon.testplatform.common.Result;
import com.sugon.testplatform.entity.MailConfig;
import com.sugon.testplatform.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/mail")
@RequiredArgsConstructor
public class MailController {
    private final MailService mailService;

    @GetMapping("/config")
    public Result<MailConfig> getConfig() {
        return Result.ok(mailService.getConfig());
    }

    @PostMapping("/config")
    public Result<Void> saveConfig(@RequestBody MailConfig config) {
        mailService.saveConfig(config);
        return Result.ok();
    }

    @PostMapping("/test")
    public Result<Void> test(@RequestBody Map<String, String> body) {
        String err = mailService.sendTest(body.get("toAddr"));
        if (err != null) throw new BizException("发送失败: " + err);
        return Result.ok();
    }
}
