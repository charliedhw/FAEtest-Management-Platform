package com.sugon.testplatform.controller;

import com.sugon.testplatform.common.PageResult;
import com.sugon.testplatform.common.Result;
import com.sugon.testplatform.entity.NotifyMsg;
import com.sugon.testplatform.security.UserContext;
import com.sugon.testplatform.service.NotifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notify")
@RequiredArgsConstructor
public class NotifyController {
    private final NotifyService notifyService;

    @GetMapping("/page")
    public Result<PageResult<NotifyMsg>> page(@RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "10") int pageSize,
                                              @RequestParam(required = false) Integer isRead) {
        return Result.ok(notifyService.myMessages(UserContext.getUserId(), pageNum, pageSize, isRead));
    }

    @GetMapping("/unreadCount")
    public Result<Long> unreadCount() {
        return Result.ok(notifyService.unreadCount(UserContext.getUserId()));
    }

    @PostMapping("/read/{id}")
    public Result<Void> markRead(@PathVariable Long id) {
        notifyService.markRead(id);
        return Result.ok();
    }

    @PostMapping("/readAll")
    public Result<Void> markAllRead() {
        notifyService.markAllRead(UserContext.getUserId());
        return Result.ok();
    }
}
