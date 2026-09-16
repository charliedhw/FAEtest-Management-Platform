package com.sugon.testplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sugon.testplatform.common.BizException;
import com.sugon.testplatform.entity.MailConfig;
import com.sugon.testplatform.entity.MailLog;
import com.sugon.testplatform.mapper.MailConfigMapper;
import com.sugon.testplatform.mapper.MailLogMapper;
import com.sugon.testplatform.security.UserContext;
import com.sugon.testplatform.service.DictService;
import com.sugon.testplatform.service.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

@Slf4j
@Service
public class MailServiceImpl implements MailService {
    private final MailConfigMapper configMapper;
    private final MailLogMapper logMapper;
    private final DictService dictService;

    public MailServiceImpl(MailConfigMapper configMapper, MailLogMapper logMapper, DictService dictService) {
        this.configMapper = configMapper;
        this.logMapper = logMapper;
        this.dictService = dictService;
    }

    @Override
    public MailConfig getConfig() {
        UserContext.requireRole("ADMIN");
        MailConfig c = configMapper.selectOne(new LambdaQueryWrapper<MailConfig>().last("limit 1"));
        if (c != null) c.setPassword(null); // 不回显授权码
        return c;
    }

    @Override
    public void saveConfig(MailConfig config) {
        UserContext.requireRole("ADMIN");
        MailConfig exist = configMapper.selectOne(new LambdaQueryWrapper<MailConfig>().last("limit 1"));
        if (exist != null) {
            config.setId(exist.getId());
            // 授权码留空表示不修改
            if (!StringUtils.hasText(config.getPassword())) config.setPassword(exist.getPassword());
            configMapper.updateById(config);
        } else {
            configMapper.insert(config);
        }
    }

    private JavaMailSenderImpl buildSender(MailConfig c) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(c.getHost());
        sender.setPort(c.getPort() == null ? 465 : c.getPort());
        sender.setUsername(c.getUsername());
        sender.setPassword(c.getPassword());
        sender.setDefaultEncoding("UTF-8");
        Properties p = sender.getJavaMailProperties();
        p.put("mail.smtp.auth", "true");
        p.put("mail.smtp.timeout", "10000");
        p.put("mail.smtp.connectiontimeout", "10000");
        if (c.getUseSsl() != null && c.getUseSsl() == 1) {
            p.put("mail.smtp.ssl.enable", "true");
            p.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            p.put("mail.smtp.socketFactory.port", String.valueOf(c.getPort() == null ? 465 : c.getPort()));
        } else {
            p.put("mail.smtp.starttls.enable", "true");
        }
        return sender;
    }

    private String baseUrl() {
        String url = dictService.getConfig("platform.base-url", "http://localhost:6080");
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    @Override
    public String sendTest(String toAddr) {
        UserContext.requireRole("ADMIN");
        MailConfig c = configMapper.selectOne(new LambdaQueryWrapper<MailConfig>().last("limit 1"));
        if (c == null || !StringUtils.hasText(c.getHost())) return "请先配置SMTP";
        try {
            doSend(c, toAddr, "测试平台邮件配置验证", "这是一封测试邮件，收到即说明邮件配置正确。\n" + baseUrl());
            return null;
        } catch (Exception e) {
            log.error("发送测试邮件失败", e);
            return e.getMessage();
        }
    }

    private void doSend(MailConfig c, String to, String subject, String content) throws Exception {
        MimeMessage msg = buildSender(c).createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
        helper.setTo(to);
        String from = StringUtils.hasText(c.getFromAddr()) ? c.getFromAddr() : c.getUsername();
        if (StringUtils.hasText(c.getFromName())) {
            helper.setFrom(from, c.getFromName());
        } else {
            helper.setFrom(from);
        }
        helper.setSubject(subject);
        helper.setText(content, false);
        buildSender(c).send(msg);
    }

    @Override
    @Async
    public void sendNotify(String toAddr, String toName, String title, String action, String jumpUrl, String bizType, Long bizId) {
        MailLog mlog = new MailLog();
        mlog.setToAddr(toAddr);
        mlog.setSubject("【测试平台】" + title);
        mlog.setBizType(bizType);
        mlog.setBizId(bizId);
        // 未启用或无收件邮箱则记录跳过
        MailConfig c = configMapper.selectOne(new LambdaQueryWrapper<MailConfig>().last("limit 1"));
        if (c == null || c.getEnabled() == null || c.getEnabled() != 1 || !StringUtils.hasText(c.getHost())) {
            mlog.setSuccess(0); mlog.setError("邮件未启用或未配置"); logMapper.insert(mlog); return;
        }
        if (!StringUtils.hasText(toAddr)) {
            mlog.setSuccess(0); mlog.setError("收件人无邮箱"); logMapper.insert(mlog); return;
        }
        String link = baseUrl() + (StringUtils.hasText(jumpUrl) ? jumpUrl : "");
        String content = (StringUtils.hasText(toName) ? toName : "") + "，您好：\n\n"
                + title + "。\n" + (StringUtils.hasText(action) ? action + "\n" : "")
                + "\n处理地址：" + link + "\n\n（本邮件由系统自动发送，请勿回复）";
        mlog.setContent(content);
        try {
            doSend(c, toAddr, "【测试平台】" + title, content);
            mlog.setSuccess(1);
        } catch (Exception e) {
            log.error("发送流程邮件失败 to={}", toAddr, e);
            mlog.setSuccess(0);
            mlog.setError(e.getMessage() == null ? "发送失败" : (e.getMessage().length() > 480 ? e.getMessage().substring(0, 480) : e.getMessage()));
        }
        logMapper.insert(mlog);
    }
}
