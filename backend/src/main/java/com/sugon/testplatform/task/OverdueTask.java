package com.sugon.testplatform.task;

import com.sugon.testplatform.entity.ResourceLoan;
import com.sugon.testplatform.service.DictService;
import com.sugon.testplatform.service.NotifyService;
import com.sugon.testplatform.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueTask {
    private final ResourceService resourceService;
    private final DictService dictService;
    private final NotifyService notifyService;

    /**
     * 每天上午9点检查超期借用，发送跟催提醒
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkOverdue() {
        log.info("开始检查超期借用...");
        List<ResourceLoan> overdueList = resourceService.overdueList();
        int warnDays = Integer.parseInt(dictService.getConfig("loan.warn.days", "30"));
        int forceDays = Integer.parseInt(dictService.getConfig("loan.force.days", "365"));

        for (ResourceLoan loan : overdueList) {
            long days = ChronoUnit.DAYS.between(loan.getLoanTime(), LocalDateTime.now());
            if (days >= forceDays) {
                // 超12个月，考核预警
                notifyService.sendToRole("BUSINESS", "借用超期考核预警",
                        "借用人【" + loan.getBorrowerName() + "】的资源借用已超过" + forceDays + "天(" + days + "天)，触发考核预警，请处理。",
                        "OVERDUE", loan.getId(), "/resource/loan");
                notifyService.send(loan.getBorrowerId(), "借用超期考核预警",
                        "您的资源借用已超过" + forceDays + "天，将按制度考核，请尽快归还。",
                        "OVERDUE", loan.getId(), "/resource/loan");
            } else if (days >= warnDays) {
                // 超30天，跟催提醒
                notifyService.sendToRole("BUSINESS", "借用跟催提醒",
                        "借用人【" + loan.getBorrowerName() + "】的资源借用已超" + warnDays + "天(" + days + "天)，请跟催归还。",
                        "OVERDUE", loan.getId(), "/resource/loan");
                notifyService.send(loan.getBorrowerId(), "借用归还提醒",
                        "您的资源借用已超" + warnDays + "天，请尽快归还或更新测试状态。",
                        "OVERDUE", loan.getId(), "/resource/loan");
            }
        }
        log.info("超期检查完成，共{}条超期记录", overdueList.size());
    }
}
