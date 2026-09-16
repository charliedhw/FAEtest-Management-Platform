package com.sugon.testplatform.service;

import com.sugon.testplatform.entity.MailConfig;

public interface MailService {
    MailConfig getConfig();
    void saveConfig(MailConfig config);
    // 发送测试邮件（同步，返回错误信息或null成功）
    String sendTest(String toAddr);
    // 异步发送流程通知邮件，bizId关联业务，jumpUrl为平台内路径(如 /approval)
    void sendNotify(String toAddr, String toName, String title, String action, String jumpUrl, String bizType, Long bizId);
}
